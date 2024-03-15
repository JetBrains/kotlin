// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.modules.decompiler.exps;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassWriter;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.ExprProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.StatEdge;
import org.jetbrains.java.decompiler.modules.decompiler.stats.DummyExitStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.CheckTypesResult;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.consts.PrimitiveConstant;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericClassDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericMethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.ListStack;
import org.jetbrains.java.decompiler.util.TextBuffer;

import java.util.*;

public class NewExprent extends Exprent {
  private InvocationExprent constructor;
  private final VarType newType;
  private final List<Exprent> lstDims;
  private List<Exprent> lstArrayElements = new ArrayList<>();
  private boolean directArrayInit;
  private boolean isVarArgParam;
  private boolean anonymous;
  private boolean lambda;
  private boolean methodReference = false;
  private boolean enumConst;
  private List<VarType> genericArgs = new ArrayList<>();
  private VarType inferredLambdaType = null;

  public NewExprent(VarType newType, ListStack<Exprent> stack, int arrayDim, BitSet bytecodeOffsets) {
    this(newType, getDimensions(arrayDim, stack), bytecodeOffsets);
  }

  public NewExprent(VarType newType, List<Exprent> lstDims, BitSet bytecodeOffsets) {
    super(Type.NEW);
    this.newType = newType;
    this.lstDims = lstDims;

    anonymous = false;
    lambda = false;
    if (newType.type == CodeConstants.TYPE_OBJECT && newType.arrayDim == 0) {
      ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);
      if (node != null && (node.type == ClassNode.Type.ANONYMOUS || node.type == ClassNode.Type.LAMBDA)) {
        anonymous = true;
        if (node.type == ClassNode.Type.LAMBDA) {
          lambda = true;
          methodReference = node.lambdaInformation.is_method_reference;
        }
      }
    }

    addBytecodeOffsets(bytecodeOffsets);
  }

  private static List<Exprent> getDimensions(int arrayDim, ListStack<Exprent> stack) {
    List<Exprent> lstDims = new ArrayList<>();
    for (int i = 0; i < arrayDim; i++) {
      lstDims.add(0, stack.pop());
    }
    return lstDims;
  }

  @Override
  public VarType getExprType() {
    return anonymous ? DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value).anonymousClassType : newType;
  }

  @Override
  public VarType getInferredExprType(VarType upperBound) {
    genericArgs.clear();
    if (!lambda && newType.type == CodeConstants.TYPE_OBJECT) {
      StructClass node = DecompilerContext.getStructContext().getClass(newType.value);

      if (node != null && node.getSignature() != null) {
        if (anonymous) {
          if (VarType.VARTYPE_OBJECT.equals(node.getSignature().superclass) && !node.getSignature().superinterfaces.isEmpty()) {
            return node.getSignature().superinterfaces.get(0);
          }
          return node.getSignature().superclass;
        }
        else if (newType.arrayDim == 0 && !node.getSignature().fparameters.isEmpty()) {
          GenericClassDescriptor sig = node.getSignature();
          if (constructor != null) {
            VarType ret = constructor.getInferredExprType(upperBound);
            return ret.type != CodeConstants.TYPE_VOID ? ret : getExprType();
          }
          else {
            Map<VarType, VarType> genericsMap = new HashMap<>();
            this.gatherGenerics(upperBound, sig.genericType, genericsMap);
            this.getGenericArgs(sig.fparameters, genericsMap, genericArgs);
            VarType _new = sig.genericType.remap(genericsMap);
            if (sig.genericType != _new) {
              return _new;
            }
          }
        }
      }
      else if (newType.arrayDim > 0 && !lstArrayElements.isEmpty() && newType.value.equals(VarType.VARTYPE_OBJECT.value)) {
        VarType first = lstArrayElements.get(0).getInferredExprType(null);
        if (first.type == CodeConstants.TYPE_GENVAR) {
          boolean matches = true;
          for (int i = 1; i < lstArrayElements.size(); ++i) {
            VarType type = lstArrayElements.get(i).getInferredExprType(null);
            if (!type.equals(first)) {
              matches = false;
              break;
            }
          }
          if (matches) {
            return first.resizeArrayDim(newType.arrayDim);
          }
        }
      }
    }

    if (lambda) {
      ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);

      if (node != null) {

        VarType classType = node.anonymousClassType;
        StructClass cls = DecompilerContext.getStructContext().getClass(classType.value);
        MethodDescriptor desc = MethodDescriptor.parseDescriptor(node.lambdaInformation.method_descriptor);
        StructClass methodCls = DecompilerContext.getStructContext().getClass(node.lambdaInformation.content_class_name);

        if (cls != null && cls.getSignature() != null && methodCls != null) {
          StructMethod refMethod = cls.getMethod(getLambdaMethodKey());
          StructMethod method = methodCls.getMethod(node.lambdaInformation.content_method_name, node.lambdaInformation.content_method_descriptor);

          if (method != null && refMethod != null && refMethod.getSignature() != null) {
            GenericType ret = cls.getSignature().genericType;

            HashMap<VarType, VarType> genericsMap = new HashMap<>();
            Map<VarType, List<VarType>> named = getNamedGenerics();

            gatherGenerics(upperBound, ret, genericsMap);

            HashMap<VarType, VarType> instanceMap = new HashMap<>();
            if (isMethodReference() && methodCls.getSignature() != null) {
              VarType first = ret.getArguments().get(0);
              if (constructor.getInstance() != null) {
                VarType instanceType = constructor.getInstance().getInferredExprType(null);
                if (instanceType.isGeneric()) {
                  methodCls.getSignature().genericType.mapGenVarsTo((GenericType)instanceType, instanceMap);
                }
              }
              else if (method.getSignature() != null) {
                for (int i = 0; i < method.getSignature().parameterTypes.size(); ++i) {
                  VarType mtype = method.getSignature().parameterTypes.get(i);
                  VarType rtype = refMethod.getSignature().parameterTypes.get(i);
                  if (mtype.type == CodeConstants.TYPE_GENVAR && rtype.type == CodeConstants.TYPE_GENVAR) {
                    if (genericsMap.containsKey(rtype)) {
                      instanceMap.put(mtype, genericsMap.get(rtype));
                    }
                  }
                }
              }
            }

            // generated lambda methods have no generic info, so only map to generic var parameters
            List<VarType> types = method.getSignature() != null ? method.getSignature().parameterTypes : Arrays.asList(desc.params);
            for (int i = 0; i < types.size(); ++i) {
              if (refMethod.getSignature().parameterTypes.get(i).type == CodeConstants.TYPE_GENVAR) {
                if (!genericsMap.containsKey(refMethod.getSignature().parameterTypes.get(i))) {
                  VarType realType = types.get(i);
                  StructClass typeCls = DecompilerContext.getStructContext().getClass(realType.value);
                  if (typeCls != null && typeCls.getSignature() != null && !realType.equals(typeCls.getSignature().genericType)) {
                    realType = typeCls.getSignature().genericType.resizeArrayDim(realType.arrayDim);
                  }
                  genericsMap.put(refMethod.getSignature().parameterTypes.get(i), realType);
                }
              }
            }

            if (refMethod.getSignature().returnType.type == CodeConstants.TYPE_GENVAR) {
              VarType key = refMethod.getSignature().returnType;
              if (method.getName().equals(CodeConstants.INIT_NAME)) {
                if (methodCls.getSignature() != null) {
                  genericsMap.put(key, methodCls.getSignature().genericType);
                }
                else {
                  genericsMap.put(key, GenericType.parse("L" + methodCls.qualifiedName + ";"));
                }
              }
              else if (method.getSignature() != null || !desc.ret.equals(VarType.VARTYPE_OBJECT)) {
                VarType current = genericsMap.get(key);
                VarType returnType = method.getSignature() != null ? method.getSignature().returnType.remap(instanceMap) : desc.ret;
                StructClass retCls = returnType == null ? null : DecompilerContext.getStructContext().getClass(returnType.value);

                if (!isMethodReference() && retCls != null && retCls.getSignature() != null && !retCls.getSignature().genericType.equalsExact(returnType)) {
                  VarType retUB = current != null && current.equals(returnType) ? current : returnType;
                  VarType realType = getLambdaReturnType(node, refMethod, retUB, genericsMap);
                  if (realType != null) {
                    returnType = realType;
                  }
                }

                boolean add = current == null || returnType == null || returnType.isGeneric() ||
                  (!returnType.equals(genericsMap.get(key)) && (current.type != CodeConstants.TYPE_GENVAR || !named.containsKey(current)));
                if (add) {
                  genericsMap.put(key, returnType);
                }
              }
            }

            ret.getAllGenericVars().forEach(from -> {
              genericsMap.putIfAbsent(from, GenericType.DUMMY_VAR);
            });

            if (!genericsMap.isEmpty()) {
              VarType _new = ret.remap(genericsMap);
              if (_new != ret) {
                if (!_new.isGeneric() || !((GenericType)_new).hasUnknownGenericType(named.keySet())) {
                  inferredLambdaType = _new;
                }
                return _new;
              }
            }
          }
        }
        else {
          inferredLambdaType = classType;
        }
      }
    }

    return getExprType();
  }

  @Override
  public CheckTypesResult checkExprTypeBounds() {
    CheckTypesResult result = new CheckTypesResult();

    if (newType.arrayDim != 0) {
      for (Exprent dim : lstDims) {
        result.addMinTypeExprent(dim, VarType.VARTYPE_BYTECHAR);
        result.addMaxTypeExprent(dim, VarType.VARTYPE_INT);
      }

      if (newType.arrayDim == 1) {
        VarType leftType = newType.decreaseArrayDim();
        for (Exprent element : lstArrayElements) {
          result.addMinTypeExprent(element, VarType.getMinTypeInFamily(leftType.typeFamily));
          result.addMaxTypeExprent(element, leftType);
        }
      }
    }
    else if (constructor != null) {
      return constructor.checkExprTypeBounds();
    }

    return result;
  }

  @Override
  public List<Exprent> getAllExprents(List<Exprent> lst) {
    if (newType.arrayDim != 0) {
      lst.addAll(lstDims);
      lst.addAll(lstArrayElements);
    }
    else if (constructor != null) {
      Exprent constructor = this.constructor.getInstance();
      if (constructor != null) { // should be true only for a lambda expression with a virtual content method
        lst.add(constructor);
      }
      lst.addAll(this.constructor.getLstParameters());
    }

    return lst;
  }

  @Override
  public Exprent copy() {
    List<Exprent> lst = new ArrayList<>();
    for (Exprent expr : lstDims) {
      lst.add(expr.copy());
    }

    NewExprent ret = new NewExprent(newType, lst, bytecode);
    ret.setConstructor(constructor == null ? null : (InvocationExprent)constructor.copy());
    ret.setLstArrayElements(lstArrayElements);
    ret.setDirectArrayInit(directArrayInit);
    ret.setAnonymous(anonymous);
    ret.setEnumConst(enumConst);
    return ret;
  }

  @Override
  public int getPrecedence() {
    return 1; // precedence of new
  }

  @Override
  public TextBuffer toJava(int indent) {
    TextBuffer buf = new TextBuffer();

    if (anonymous) {
      ClassNode child = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);

      boolean selfReference = DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE) == child;

      // IDEA-204310 - avoid backtracking later on for lambdas (causes spurious imports)
      if (!enumConst && (!lambda || DecompilerContext.getOption(IFernflowerPreferences.LAMBDA_TO_ANONYMOUS_CLASS))) {
        TextBuffer enclosing = null;

        if (!lambda && constructor != null) {
          enclosing = getQualifiedNewInstance(child.anonymousClassType.value, constructor.getLstParameters(), indent);
          if (enclosing != null) {
            buf.append(enclosing).append('.');
          }
        }

        buf.append("new ");

        if (selfReference) {
          buf.append("<anonymous constructor>");
        } else {
          String typename = ExprProcessor.getCastTypeName(child.anonymousClassType);
          if (enclosing != null) {
            ClassNode anonymousNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(child.anonymousClassType.value);
            if (anonymousNode != null) {
              typename = anonymousNode.simpleName;
            }
            else {
              typename = typename.substring(typename.lastIndexOf('.') + 1);
            }
          }

          boolean appendType = true;
          if (child.getWrapper() != null) {
            GenericClassDescriptor descriptor = child.getWrapper().getClassStruct().getSignature();
            if (descriptor != null) {
              if (descriptor.superinterfaces.isEmpty()) {
                buf.append(ExprProcessor.getCastTypeName(descriptor.superclass));
              } else {
                if (descriptor.superinterfaces.size() > 1 && !lambda) {
                  DecompilerContext.getLogger().writeMessage("Inconsistent anonymous class signature: " + child.classStruct.qualifiedName,
                    IFernflowerLogger.Severity.WARN);
                }
                buf.append(ExprProcessor.getCastTypeName(descriptor.superinterfaces.get(0)));
              }
              appendType = false;
            }
          }

          if (appendType) {
            buf.append(typename);
          }
        }
      }

      if (!lambda && constructor != null) {
        appendParameters(buf, constructor.getGenericArgs());
        buf.append('(').append(constructor.appendParamList(indent));
      }
      else {
        appendParameters(buf, genericArgs);
        buf.append('(');
      }

      buf.append(')');

      if (enumConst && buf.length() == 2) {
        buf.setLength(0);
      }

      if (lambda) {
        if (!DecompilerContext.getOption(IFernflowerPreferences.LAMBDA_TO_ANONYMOUS_CLASS)) {
          buf.setLength(0);  // remove the usual 'new <class>()', it will be replaced with lambda style '() ->'
        }
        setLambdaGenericTypes();
        Exprent methodObject = constructor == null ? null : constructor.getInstance();
        new ClassWriter().classLambdaToJava(child, buf, methodObject, indent);
      }
      else if (!selfReference) {
        new ClassWriter().classToJava(child, buf, indent);
      }
    }
    else if (directArrayInit) {
      VarType leftType = newType.decreaseArrayDim();
      buf.append('{');
      if (!lstArrayElements.isEmpty()) {
        buf.pushNewlineGroup(indent, 2);
        buf.appendPossibleNewline();
        buf.pushNewlineGroup(indent, 0);
      }
      for (int i = 0; i < lstArrayElements.size(); i++) {
        if (i > 0) {
          buf.append(",").appendPossibleNewline(" ");
        }
        ExprProcessor.getCastedExprent(lstArrayElements.get(i), leftType, buf, indent, false);
      }
      if (!lstArrayElements.isEmpty()) {
        buf.popNewlineGroup();
        buf.appendPossibleNewline("", true);
        buf.popNewlineGroup();
      }
      buf.append('}');
    }
    else if (newType.arrayDim == 0) {
      if (!enumConst) {
        TextBuffer enclosing = null;

        if (constructor != null) {
          enclosing = getQualifiedNewInstance(newType.value, constructor.getLstParameters(), indent);
          if (enclosing != null) {
            buf.append(enclosing).append('.');
          }
        }

        buf.append("new ");

        String typename = ExprProcessor.getTypeName(newType);
        if (enclosing != null) {
          ClassNode newNode = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);
          if (newNode != null) {
            typename = newNode.simpleName;
          }
          else {
            typename = typename.substring(typename.lastIndexOf('.') + 1);
          }
        }
        buf.append(typename);
      }

      if (constructor != null) {
        int start = enumConst ? 2 : 0;
        if (!enumConst || start < constructor.getLstParameters().size()) {
          appendParameters(buf, constructor.getGenericArgs());
          buf.append('(').append(constructor.appendParamList(indent)).append(')');
        }
      }
    }
    else if (isVarArgParam) {
      // just print the array elements
      VarType leftType = newType.decreaseArrayDim();
      for (int i = 0; i < lstArrayElements.size(); i++) {
        if (i > 0) {
          buf.append(",").appendPossibleNewline(" ");
        }

        // new String[][]{{"abc"}, {"DEF"}} => new String[]{"abc"}, new String[]{"DEF"}
        Exprent element = lstArrayElements.get(i);
        if (element instanceof NewExprent) {
          ((NewExprent) element).setDirectArrayInit(false);
        }
        ExprProcessor.getCastedExprent(element, leftType, buf, indent, false);
      }

      // if there is just one element of Object[] type it needs to be casted to resolve ambiguity
      if (lstArrayElements.size() == 1) {
        VarType elementType = lstArrayElements.get(0).getExprType();
        if (elementType.type == CodeConstants.TYPE_OBJECT && elementType.value.equals("java/lang/Object") && elementType.arrayDim >= 1) {
          buf.prepend("(Object)");
        }
      }
    }
    else {
      buf.append("new ").append(ExprProcessor.getTypeName(newType));

      if (lstArrayElements.isEmpty()) {
        for (int i = 0; i < newType.arrayDim; i++) {
          buf.append('[');
          if (i < lstDims.size()) {
            if (lstDims.get(i).type == Type.CONST) {
              ((ConstExprent)lstDims.get(i)).adjustConstType(VarType.VARTYPE_INT);
            }
            buf.append(lstDims.get(i).toJava(indent));
          }
          buf.append(']');
        }
      }
      else {
        for (int i = 0; i < newType.arrayDim; i++) {
          buf.append("[]");
        }

        VarType leftType = newType.decreaseArrayDim();
        buf.append('{');
        buf.pushNewlineGroup(indent, 1);
        buf.appendPossibleNewline();
        buf.pushNewlineGroup(indent, 0);
        for (int i = 0; i < lstArrayElements.size(); i++) {
          if (i > 0) {
            buf.append(",").appendPossibleNewline(" ");
          }
          ExprProcessor.getCastedExprent(lstArrayElements.get(i), leftType, buf, indent, false);
        }
        buf.popNewlineGroup();
        buf.appendPossibleNewline("", true);
        buf.append('}');
        buf.popNewlineGroup();
      }
    }

    return buf;
  }

  // TODO move to InvocationExprent
  public static boolean probablySyntheticParameter(String className) {
    ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(className);
    return node != null && node.type == ClassNode.Type.ANONYMOUS;
  }

  private static TextBuffer getQualifiedNewInstance(String classname, List<Exprent> lstParams, int indent) {
    ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(classname);

    if (node != null && node.type != ClassNode.Type.ROOT && node.type != ClassNode.Type.LOCAL
        && (node.access & CodeConstants.ACC_STATIC) == 0) {
      if (!lstParams.isEmpty()) {
        Exprent enclosing = lstParams.get(0);

        boolean isQualifiedNew = false;

        if (enclosing instanceof VarExprent) {
          VarExprent varEnclosing = (VarExprent)enclosing;

          StructClass current_class = ((ClassNode)DecompilerContext.getProperty(DecompilerContext.CURRENT_CLASS_NODE)).classStruct;
          String this_classname = varEnclosing.getProcessor().getThisVars().get(new VarVersionPair(varEnclosing));

          if (!current_class.qualifiedName.equals(this_classname)) {
            isQualifiedNew = true;
          }
        }
        else {
          isQualifiedNew = true;
        }

        if (isQualifiedNew) {
          return enclosing.toJava(indent);
        }
      }
    }

    return null;
  }

  private static VarType getLambdaReturnType(ClassNode node, StructMethod desc, VarType upperBound, Map<VarType, VarType> genericsMap) {
    ClassWrapper wrapper = node.getWrapper();
    Map<String, VarType> inferredLambdaTypes = Exprent.inferredLambdaTypes.get();

    if (wrapper != null) {
      MethodWrapper mt = wrapper.getMethodWrapper(node.lambdaInformation.content_method_name, node.lambdaInformation.content_method_descriptor);

      if (mt != null && mt.root != null) {
        List<String> paramNames = new ArrayList<>();

        MethodDescriptor md_content = MethodDescriptor.parseDescriptor(node.lambdaInformation.content_method_descriptor);
        MethodDescriptor md_lambda = MethodDescriptor.parseDescriptor(node.lambdaInformation.method_descriptor);

        int index = node.lambdaInformation.is_content_method_static ? 0 : 1;
        int start_index = md_content.params.length - md_lambda.params.length;

        int j = 0;
        for (int i = 0; i < md_content.params.length; i++) {
          if (i >= start_index) {
            VarVersionPair vpp = new VarVersionPair(index, 0);
            VarType curType = mt.varproc.getVarType(vpp);
            VarType infType = desc.getSignature().parameterTypes.get(j++).remap(genericsMap);

            if (infType != null && !infType.equals(VarType.VARTYPE_VOID)) {
              if (!curType.equals(infType) || (infType.isGeneric() && !((GenericType)infType).equalsExact(curType))) {
                String varName = mt.varproc.getVarName(vpp);
                paramNames.add(varName);
                inferredLambdaTypes.put(varName, infType);
              }
            }
          }
          index += md_content.params[i].stackSize;
        }

        DummyExitStatement dummyExit = mt.root.getDummyExit();

        for (StatEdge edge : dummyExit.getAllPredecessorEdges()) {
          Statement source = edge.getSource();
          List<Exprent> lstExpr = source.getExprents();

          if (lstExpr != null && !lstExpr.isEmpty()) {
            Exprent expr = lstExpr.get(lstExpr.size() - 1);
            if (expr instanceof ExitExprent) {
              ExitExprent ex = (ExitExprent)expr;
              if (ex.getExitType() == ExitExprent.Type.RETURN) {
                VarType realRetType = ex.getValue().getInferredExprType(upperBound);
                if (realRetType.isGeneric()) {
                  paramNames.forEach(inferredLambdaTypes::remove);
                  return realRetType;
                }
              }
            }
          }
        }

        paramNames.forEach(inferredLambdaTypes::remove);
      }
    }
    return null;
  }

  private void setLambdaGenericTypes() {
    if (inferredLambdaType != null) {
      ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);
      StructClass cls = DecompilerContext.getStructContext().getClass(inferredLambdaType.value);

      if (node != null && cls != null) {
        StructMethod desc = cls.getMethod(getLambdaMethodKey());
        ClassWrapper wrapper = node.getWrapper();
        MethodWrapper methodWrapper = wrapper != null ? wrapper.getMethodWrapper(node.lambdaInformation.content_method_name, node.lambdaInformation.content_method_descriptor) : null;

        if (desc != null && desc.getSignature() != null && methodWrapper != null && methodWrapper.root != null) {
          if (!desc.getClassQualifiedName().equals(inferredLambdaType.value)) {
            StructClass candidate = DecompilerContext.getStructContext().getClass(desc.getClassQualifiedName());
            if (candidate.getSignature() != null) {
              cls = candidate;
              inferredLambdaType = GenericType.getGenericSuperType(inferredLambdaType, candidate.getSignature().genericType);
            }
          }

          Map<VarType, VarType> tempMap = new HashMap<>();
          if (inferredLambdaType.isGeneric()) {
            cls.getSignature().genericType.mapGenVarsTo((GenericType)inferredLambdaType, tempMap);
          }

          MethodDescriptor md_content = MethodDescriptor.parseDescriptor(node.lambdaInformation.content_method_descriptor);
          MethodDescriptor md_lambda = MethodDescriptor.parseDescriptor(node.lambdaInformation.method_descriptor);

          int index = node.lambdaInformation.is_content_method_static ? 0 : 1;
          int start_index = md_content.params.length - md_lambda.params.length;

          int j = 0;
          for (int i = 0; i < md_content.params.length; i++) {
            if (i >= start_index) {
              VarVersionPair vpp = new VarVersionPair(index, 0);
              VarType curType = methodWrapper.varproc.getVarType(vpp);
              VarType infType = desc.getSignature().parameterTypes.get(j++).remap(tempMap);

              if (infType != null && !infType.equals(VarType.VARTYPE_VOID)) {
                if (!curType.equals(infType) || (infType.isGeneric() && !((GenericType)infType).equalsExact(curType))) {
                  methodWrapper.varproc.setVarType(vpp, infType);
                  String paramName = methodWrapper.varproc.getVarName(vpp);

                  LinkedList<ClassNode> nested = new LinkedList<>(node.nested);
                  while (!nested.isEmpty()) {
                    ClassNode childNode = nested.removeFirst();
                    nested.addAll(childNode.nested);

                    if (childNode.type == ClassNode.Type.LAMBDA && !childNode.lambdaInformation.is_method_reference) {
                      MethodWrapper enclosedMethod = wrapper.getMethodWrapper(childNode.lambdaInformation.content_method_name, childNode.lambdaInformation.content_method_descriptor);

                      if (enclosedMethod != null && paramName.equals(enclosedMethod.varproc.getVarName(vpp))) {
                        enclosedMethod.varproc.setVarType(vpp, infType);
                      }
                    }
                  }
                }
              }
            }
            index += md_content.params[i].stackSize;
          }

          VarType curType = md_content.ret;
          VarType infType = desc.getSignature().returnType.remap(tempMap);

          if (infType != null && !infType.equals(VarType.VARTYPE_VOID)) {
            if (!curType.equals(infType) || (infType.isGeneric() && !((GenericType)infType).equalsExact(curType))) {
              GenericMethodDescriptor genDesc = new GenericMethodDescriptor(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), infType, Collections.emptyList());
              DummyExitStatement dummyExit = methodWrapper.root.getDummyExit();

              for (StatEdge edge : dummyExit.getAllPredecessorEdges()) {
                Statement source = edge.getSource();
                List<Exprent> lstExpr = source.getExprents();

                if (lstExpr != null && !lstExpr.isEmpty()) {
                  Exprent expr = lstExpr.get(lstExpr.size() - 1);
                  if (expr instanceof ExitExprent) {
                    ExitExprent ex = (ExitExprent)expr;
                    if (ex.getExitType() == ExitExprent.Type.RETURN) {
                      ex.getMethodDescriptor().genericInfo = genDesc;
                      break; // desc var should be the same for all returns
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  public boolean doesClassHaveMethodsNamedSame() {
    if (this.lambda && this.methodReference) {
      ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(this.newType.value);

      if (node != null) {
        String name = node.lambdaInformation.content_method_name;
        ClassNode contentClass = DecompilerContext.getClassProcessor().getMapRootClasses().get(node.lambdaInformation.content_class_name);

        if (contentClass == null) {
          return false;
        }

        boolean foundOne = false;
        for (StructMethod method : contentClass.classStruct.getMethods()) {
          if (method.getName().equals(name)) {
            if (foundOne) {
              return true;
            } else {
              foundOne = true;
            }
          }
        }
      }
    }

    return false;
  }

  @Override
  public void replaceExprent(Exprent oldExpr, Exprent newExpr) {
    if (oldExpr == constructor) {
      constructor = (InvocationExprent)newExpr;
    }

    if (constructor != null) {
      constructor.replaceExprent(oldExpr, newExpr);
    }

    for (int i = 0; i < lstDims.size(); i++) {
      if (oldExpr == lstDims.get(i)) {
        lstDims.set(i, newExpr);
      }
    }

    for (int i = 0; i < lstArrayElements.size(); i++) {
      if (oldExpr == lstArrayElements.get(i)) {
        lstArrayElements.set(i, newExpr);
      }
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof NewExprent)) return false;

    NewExprent ne = (NewExprent)o;
    return InterpreterUtil.equalObjects(newType, ne.getNewType()) &&
           InterpreterUtil.equalLists(lstDims, ne.getLstDims()) &&
           InterpreterUtil.equalObjects(constructor, ne.getConstructor()) &&
           directArrayInit == ne.directArrayInit &&
           InterpreterUtil.equalLists(lstArrayElements, ne.getLstArrayElements());
  }

  @Override
  public void getBytecodeRange(BitSet values) {
    measureBytecode(values, lstArrayElements);
    measureBytecode(values, lstDims);
    measureBytecode(values, constructor);
    measureBytecode(values);
  }

  public InvocationExprent getConstructor() {
    return constructor;
  }

  public void setConstructor(InvocationExprent constructor) {
    this.constructor = constructor;
  }

  public List<Exprent> getLstDims() {
    return lstDims;
  }

  public VarType getNewType() {
    return newType;
  }

  public List<Exprent> getLstArrayElements() {
    return lstArrayElements;
  }

  public void setLstArrayElements(List<Exprent> lstArrayElements) {
    this.lstArrayElements = lstArrayElements;
  }

  public void setDirectArrayInit(boolean directArrayInit) {
    this.directArrayInit = directArrayInit;
  }

  public boolean isDirectArrayInit() {
    return directArrayInit;
  }

  public void setVarArgParam(boolean isVarArgParam) {
    this.isVarArgParam = isVarArgParam;
  }

  public boolean isLambda() {
    return lambda;
  }

  public boolean isAnonymous() {
    return anonymous;
  }

  public void setAnonymous(boolean anonymous) {
    this.anonymous = anonymous;
  }

  public void setEnumConst(boolean enumConst) {
    this.enumConst = enumConst;
  }

  public boolean isMethodReference() {
    return methodReference;
  }

  public String getLambdaMethodKey() {
    ClassNode node = DecompilerContext.getClassProcessor().getMapRootClasses().get(newType.value);
    if (node != null && constructor != null) {
      String descriptor = ((PrimitiveConstant)constructor.getBootstrapArguments().get(0)).getString();
      return InterpreterUtil.makeUniqueKey(node.lambdaInformation.method_name, descriptor);
    }
    return "";
  }

  public void setInvocationInstance() {
    if (constructor != null) {
      constructor.setInvocationInstance();
    }
  }
}
