// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main.rels;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.code.cfg.BasicBlock;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.collectors.VarNamesCollector;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectNode;
import org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.DoStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.Statement;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarTypeProcessor.FinalType;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructEnclosingMethodAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructLocalVariableTableAttribute.LocalVariable;
import org.jetbrains.java.decompiler.struct.consts.LinkConstant;
import org.jetbrains.java.decompiler.struct.consts.PooledConstant;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.struct.gen.generics.GenericType;
import org.jetbrains.java.decompiler.util.DotExporter;
import org.jetbrains.java.decompiler.util.InterpreterUtil;

import java.util.*;
import java.util.Map.Entry;

public class NestedClassProcessor {
  public void processClass(ClassNode root, ClassNode node) {
    // hide synthetic lambda content methods
    if (node.type == ClassNode.Type.LAMBDA && !node.lambdaInformation.is_method_reference) {
      ClassNode node_content = DecompilerContext.getClassProcessor().getMapRootClasses().get(node.classStruct.qualifiedName);
      if (node_content != null && node_content.getWrapper() != null) {
        node_content.getWrapper().getHiddenMembers().add(node.lambdaInformation.content_method_key);
      }
    }

    if (node.nested.isEmpty()) {
      return;
    }

    // lambdas can be direct children and children of other lambdas at the same time,
    // remove those from the set of direct children, so they don't get processed before their parent
    Set<ClassNode> doubleNested = new HashSet<>();
    for (ClassNode nested : node.nested) {
      doubleNested.addAll(nested.getAllNested());
    }
    node.nested.removeAll(doubleNested);

    if (node.type != ClassNode.Type.LAMBDA) {
      computeLocalVarsAndDefinitions(node);

      // for each local or anonymous class ensure not empty enclosing method
      checkNotFoundClasses(root, node);
    }

    int nameless = 0, synthetics = 0;
    for (ClassNode child : node.nested) {
      StructClass cl = child.classStruct;
      // ensure not-empty class name
      if ((child.type == ClassNode.Type.LOCAL || child.type == ClassNode.Type.MEMBER) && child.simpleName == null) {
        if ((child.access & CodeConstants.ACC_SYNTHETIC) != 0 || cl.isSynthetic()) {
          child.simpleName = "SyntheticClass_" + (++synthetics);
        }
        else {
          String message = "Nameless local or member class " + cl.qualifiedName + "!";
          DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
          child.simpleName = "NamelessClass_" + (++nameless);
        }
      }
    }

    for (ClassNode child : node.nested) {
      if (child.type == ClassNode.Type.LAMBDA) {
        setLambdaVars(node, child);
      }
      else if (child.type != ClassNode.Type.MEMBER || (child.access & CodeConstants.ACC_STATIC) == 0) {
        insertLocalVars(node, child);

        if (child.type == ClassNode.Type.LOCAL && child.enclosingMethod != null) {
          MethodWrapper enclosingMethodWrapper = node.getWrapper().getMethods().getWithKey(child.enclosingMethod);
          if(enclosingMethodWrapper != null) { // e.g. in case of switch-on-enum. FIXME: some proper handling of multiple enclosing classes 
            setLocalClassDefinition(enclosingMethodWrapper, child);
          }
        }
      }
    }

    for (ClassNode child : new ArrayList<>(node.nested)) {
      processClass(root, child);
    }
  }
  /**
   * When Java introduced Enums they aded the ability to use them in Switch statements.
   * This was done in a purely syntax sugar way using the old switch on int methods.
   * The compiler creates a synthetic class with a static int array field.
   * To support enums changing post compile, It initializes this field with a length of the current enum length.
   * And then for every referenced enum value it adds a mapping in the form of:
   *   try {
   *     field[Enum.VALUE.ordinal()] = 1;
   *   } catch (FieldNotFoundException e) {}
   *
   * If a class has multiple switches on multiple enums, the compiler adds the init and try list to the BEGINNING of the static initalizer.
   * But they add the field to the END of the fields list.
   */
  /*
  private void gatherEnumSwitchMaps(ClassNode node) {
    for (ClassNode child : node.nested) {
      gatherEnumSwitchMaps(child);
    }

    MethodWrapper clinit = node.getWrapper().getMethodWrapper("<clinit>", "()V");
    if (clinit == null || clinit.root == null || clinit.root.getFirst().type != Statement.TYPE_SEQUENCE) {
      return;
    }

    final int STATIC_FINAL_SYNTHETIC = CodeConstants.ACC_STATIC | CodeConstants.ACC_STATIC | CodeConstants.ACC_FINAL | CodeConstants.ACC_SYNTHETIC;
    Set<String> potentialFields = new HashSet<String>();
    for (StructField fd : node.classStruct.getFields()) {
      if ((fd.getAccessFlags() & STATIC_FINAL_SYNTHETIC) == STATIC_FINAL_SYNTHETIC && "[I".equals(fd.getDescriptor())) {
        potentialFields.add(fd.getName());
      }
    }

    if (potentialFields.size() == 0) {
      return;
    }

    SequenceStatement seq = (SequenceStatement)clinit.root.getFirst();
    for (int x = 0; x < seq.getStats().size();) {
      Statement stat = seq.getStats().get(x);
      if (stat.type != Statement.TYPE_BASICBLOCK || stat.getExprents() == null || stat.getExprents().size() != 1 || stat.getExprents().get(0).type != Exprent.EXPRENT_ASSIGNMENT) {
        break;
      }
      AssignmentExprent ass = (AssignmentExprent)stat.getExprents().get(0);
      if (ass.getLeft().type != Exprent.EXPRENT_FIELD || ass.getRight().type != Exprent.EXPRENT_NEW) {
        break;
      }
      FieldExprent mapField = (FieldExprent)ass.getLeft();
      NewExprent _new = ((NewExprent)ass.getRight());
      if (!mapField.getClassname().equals(node.classStruct.qualifiedName) || !potentialFields.contains(mapField.getName()) ||
          _new.getNewType().type != CodeConstants.TYPE_INT || _new.getNewType().arrayDim != 1 ||
          _new.getLstDims().size() != 1 || _new.getLstDims().get(0).type != Exprent.EXPRENT_FUNCTION) {
        break;
      }
      FunctionExprent func = (FunctionExprent)_new.getLstDims().get(0);
      if (func.getFuncType() != FunctionExprent.FUNCTION_ARRAY_LENGTH || func.getLstOperands().size() != 1 || func.getLstOperands().get(0).type != Exprent.EXPRENT_INVOCATION) {
        break;
      }
      InvocationExprent invoc = (InvocationExprent)func.getLstOperands().get(0);
      if (!"values".equals(invoc.getName()) || !("()[L" + invoc.getClassname() + ";").equals(invoc.getStringDescriptor())) {
        break;
      }

      String fieldName = mapField.getName();
      String enumName = invoc.getClassname();
      Map<Integer, String> idToName = new HashMap<Integer, String>();

      boolean replace = false;
      int y = x;
      while (++y < seq.getStats().size()) {
        if (seq.getStats().get(y).type != Statement.TYPE_TRYCATCH) {
          break;
        }
        CatchStatement _try = (CatchStatement)seq.getStats().get(y);
        Statement first = _try.getFirst();
        List<Exprent> exprents = first.getExprents();
        if (_try.getVars().size() != 1 || !"java/lang/NoSuchFieldError".equals(_try.getVars().get(0).getVarType().value) ||
            first.type != Statement.TYPE_BASICBLOCK || exprents == null || exprents.size() != 1 || exprents.get(0).type != Exprent.EXPRENT_ASSIGNMENT) {
          break;
        }
        ass = (AssignmentExprent)exprents.get(0);
        if (ass.getRight().type != Exprent.EXPRENT_CONST || (!(((ConstExprent)ass.getRight()).getValue() instanceof Integer)) ||
            ass.getLeft().type != Exprent.EXPRENT_ARRAY){
          break;
        }
        ArrayExprent array = (ArrayExprent)ass.getLeft();
        if (array.getArray().type != Exprent.EXPRENT_FIELD || !array.getArray().equals(mapField) || array.getIndex().type != Exprent.EXPRENT_INVOCATION) {
          break;
        }
        invoc = (InvocationExprent)array.getIndex();
        if (!enumName.equals(invoc.getClassname()) || !"ordinal".equals(invoc.getName()) || !"()I".equals(invoc.getStringDescriptor()) ||
            invoc.getInstance().type != Exprent.EXPRENT_FIELD) {
          break;
        }

        FieldExprent enumField = (FieldExprent)invoc.getInstance();
        if (!enumName.equals(enumField.getClassname()) || !enumField.isStatic()) {
          break;
        }

        idToName.put((Integer)((ConstExprent)ass.getRight()).getValue(), enumField.getName());
        seq.replaceStatement(_try, getNewEmptyStatement());
        replace = true;
      }

      if (replace) {
        seq.replaceStatement(seq.getStats().get(x), getNewEmptyStatement());
        node.classStruct.getEnumSwitchMap().put(fieldName, idToName);
        node.getWrapper().getHiddenMembers().add(InterpreterUtil.makeUniqueKey(fieldName, "[I"));
      }
      x = y;
    }
  }*/

  private Statement getNewEmptyStatement() {
    BasicBlockStatement bstat = new BasicBlockStatement(new BasicBlock(
      DecompilerContext.getCounterContainer().getCounterAndIncrement(CounterContainer.STATEMENT_COUNTER)));
    bstat.setExprents(new ArrayList<Exprent>());
    return bstat;
  }

  private static void setLambdaVars(ClassNode parent, ClassNode child) {
    if (child.lambdaInformation.is_method_reference) { // method reference, no code and no parameters
      return;
    }

    MethodWrapper method = parent.getWrapper().getMethods().getWithKey(child.lambdaInformation.content_method_key);
    VarProcessor varProc = method.varproc;
    if (varProc.nestedProcessed) {
      DecompilerContext.getLogger().writeMessage(parent.classStruct.qualifiedName + "." + method + " processed twice", IFernflowerLogger.Severity.WARN);
      return;
    }
    MethodWrapper enclosingMethod = parent.getWrapper().getMethods().getWithKey(child.enclosingMethod);

    // Don't process if either methods have decompiled with errors
    if (enclosingMethod.decompileError != null || method.decompileError != null) {
      return;
    }

    MethodDescriptor md_lambda = MethodDescriptor.parseDescriptor(child.lambdaInformation.method_descriptor);
    MethodDescriptor md_content = MethodDescriptor.parseDescriptor(child.lambdaInformation.content_method_descriptor);

    int vars_count = md_content.params.length - md_lambda.params.length;
    boolean is_static_lambda_content = child.lambdaInformation.is_content_method_static;

    String parent_class_name = parent.getWrapper().getClassStruct().qualifiedName;
    String lambda_class_name = child.simpleName;

    VarType lambda_class_type = new VarType(lambda_class_name, true);

    // this pointer
    if (!is_static_lambda_content && DecompilerContext.getOption(IFernflowerPreferences.LAMBDA_TO_ANONYMOUS_CLASS)) {
      varProc.getThisVars().put(new VarVersionPair(0, 0), parent_class_name);
      varProc.setVarName(new VarVersionPair(0, 0), parent.simpleName + ".this");
    }

    // collect all previously used names, excluding "this"
    VarProcessor enclosingVarProc = enclosingMethod.varproc;
    Set<String> usedBefore = new HashSet<>(enclosingVarProc.getVarNamesCollector().getUsedNames());
    usedBefore.addAll(enclosingVarProc.getVarNames());
    for (VarVersionPair thisVar : enclosingVarProc.getThisVars().keySet()) {
      usedBefore.remove(enclosingVarProc.getVarName(thisVar));
    }
    VarNamesCollector enclosingCollector = new VarNamesCollector(usedBefore);

    Map<VarVersionPair, String> mapNewNames = new HashMap<>();
    Map<VarVersionPair, LocalVariable> lvts = new HashMap<>();

    // rename colliding local variables
    for (VarVersionPair local : varProc.getUsedVarVersions()) {
      String name = null;
      LocalVariable lvt = varProc.getVarLVT(local);
      if (lvt != null) {
        name = lvt.getName();
      }
      if (name == null) {
        name = varProc.getVarName(local);
      }
      if (usedBefore.contains(name) && !"this".equals(name)) {
        name = enclosingCollector.getFreeName(name);
        mapNewNames.put(local, name);
        if (lvt != null) {
          lvts.put(local, lvt.rename(name));
        }
      }
    }

    // Possible with debug method filter enabled in ClassWrapper
    if (enclosingMethod.getOrBuildGraph() == null) {
      return;
    }

    enclosingMethod.getOrBuildGraph().iterateExprents(exprent -> {
      List<Exprent> lst = exprent.getAllExprents(true);
      lst.add(exprent);

      for (Exprent expr : lst) {
        if (expr instanceof NewExprent) {
          NewExprent new_expr = (NewExprent)expr;

          if (new_expr.isLambda() && lambda_class_type.equals(new_expr.getNewType())) {
            InvocationExprent inv_dynamic = new_expr.getConstructor();

            int param_index = is_static_lambda_content ? 0 : 1;
            int varIndex = is_static_lambda_content ? 0 : 1;

            for (int i = 0; i < md_content.params.length; ++i) {
              VarVersionPair varVersion = new VarVersionPair(varIndex, 0);
              if (i < vars_count) {
                Exprent param = inv_dynamic.getLstParameters().get(param_index + i);

                if (param instanceof VarExprent) {
                  mapNewNames.put(varVersion, enclosingVarProc.getVarName(new VarVersionPair((VarExprent)param)));
                  lvts.put(varVersion, ((VarExprent)param).getLVT());
                  if (enclosingVarProc.getVarFinal((new VarVersionPair((VarExprent)param))) == FinalType.NON_FINAL) {
                    //DecompilerContext.getLogger().writeMessage("Lambda in " + parent.simpleName + "." + enclosingMethod.methodStruct.getName() + " given non-final var " + ((VarExprent)param).getName() + "!", IFernflowerLogger.Severity.ERROR);
                  }
                }
              }
              else if (!mapNewNames.containsKey(varVersion)) {
                mapNewNames.put(varVersion, enclosingCollector.getFreeName(varProc.getVarName(varVersion)));
              }

              varIndex += md_content.params[i].stackSize;
            }
          }
        }
      }

      return 0;
    });

    // update names of local variables
    Set<String> setNewOuterNames = new HashSet<>(mapNewNames.values());
    setNewOuterNames.removeAll(method.setOuterVarNames);

    method.setOuterVarNames.addAll(setNewOuterNames);
    varProc.getVarNamesCollector().addNames(enclosingCollector.getUsedNames());

    for (Entry<VarVersionPair, String> entry : mapNewNames.entrySet()) {
      VarVersionPair pair = entry.getKey();
      LocalVariable lvt = lvts.get(pair);

      varProc.setInheritedName(pair, entry.getValue());
      if (lvt != null) {
        varProc.setVarLVT(pair, lvt);
      }
    }

    method.getOrBuildGraph().iterateExprentsDeep(exp -> {
      if (exp instanceof VarExprent) {
        VarExprent var = (VarExprent)exp;
        LocalVariable lv = lvts.get(var.getVarVersionPair());
        if (lv != null)
          var.setLVT(lv);
        else if (mapNewNames.containsKey(var.getVarVersionPair()))
          var.setLVT(null);
      }
      return 0;
    });
    varProc.nestedProcessed = true;
  }

  private static void checkNotFoundClasses(ClassNode root, ClassNode node) {
    List<ClassNode> copy = new ArrayList<>(node.nested);

    for (ClassNode child : copy) {
      if (child.classStruct.isSynthetic()) {
        continue;
      }

      if ((child.type == ClassNode.Type.LOCAL || child.type == ClassNode.Type.ANONYMOUS) && child.enclosingMethod == null) {
        Set<String> setEnclosing = child.enclosingClasses;

        if (!setEnclosing.isEmpty()) {
          StructEnclosingMethodAttribute attr = child.classStruct.getAttribute(StructGeneralAttribute.ATTRIBUTE_ENCLOSING_METHOD);
          if (attr != null &&
              attr.getMethodName() != null &&
              node.classStruct.qualifiedName.equals(attr.getClassName()) &&
              node.classStruct.getMethod(attr.getMethodName(), attr.getMethodDescriptor()) != null) {
            child.enclosingMethod = InterpreterUtil.makeUniqueKey(attr.getMethodName(), attr.getMethodDescriptor());
            continue;
          }
        }

        node.nested.remove(child);
        child.parent = null;
        setEnclosing.remove(node.classStruct.qualifiedName);

        boolean hasEnclosing = !setEnclosing.isEmpty() && insertNestedClass(root, child);

        if (!hasEnclosing) {
          if (child.type == ClassNode.Type.ANONYMOUS) {
            String message = "Unreferenced anonymous class " + child.classStruct.qualifiedName + "!";
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
          }
          else if (child.type == ClassNode.Type.LOCAL) {
            String message = "Unreferenced local class " + child.classStruct.qualifiedName + "!";
            DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
          }
        }
      }
    }
  }

  private static boolean insertNestedClass(ClassNode root, ClassNode child) {
    Set<String> setEnclosing = child.enclosingClasses;

    LinkedList<ClassNode> stack = new LinkedList<>();
    stack.add(root);

    while (!stack.isEmpty()) {
      ClassNode node = stack.removeFirst();

      if (setEnclosing.contains(node.classStruct.qualifiedName)) {
        node.nested.add(child);
        child.parent = node;
        Collections.sort(node.nested);

        return true;
      }

      // note: ordered list
      stack.addAll(node.nested);
    }

    return false;
  }

  private static void computeLocalVarsAndDefinitions(ClassNode node) {
    // class name -> constructor descriptor -> var to field link
    Map<String, Map<String, List<VarFieldPair>>> mapVarMasks = new HashMap<>();

    Set<ClassNode.Type> clTypes = EnumSet.noneOf(ClassNode.Type.class);

    for (ClassNode nd : node.nested) {
      if (nd.type != ClassNode.Type.LAMBDA &&
          !nd.classStruct.isSynthetic() &&
          (nd.access & CodeConstants.ACC_STATIC) == 0 &&
          (nd.access & CodeConstants.ACC_INTERFACE) == 0) {
        clTypes.add(nd.type);

        Map<String, List<VarFieldPair>> mask = getMaskLocalVars(nd.getWrapper());
        if (mask.isEmpty()) {
          String message = "Nested class " + nd.classStruct.qualifiedName + " has no constructor!";
          DecompilerContext.getLogger().writeMessage(message, IFernflowerLogger.Severity.WARN);
        }
        else {
          mapVarMasks.put(nd.classStruct.qualifiedName, mask);
        }
      }
    }

    // local var masks
    Map<String, Map<String, List<VarFieldPair>>> mapVarFieldPairs = new HashMap<>();

    if (!clTypes.equals(EnumSet.of(ClassNode.Type.MEMBER))) {
      // iterate enclosing class
      for (MethodWrapper method : node.getWrapper().getMethods()) {
        if (method.root != null) { // neither abstract, nor native
          DotExporter.toDotFile(method.getOrBuildGraph(), method.methodStruct, "computeLocalVars");
          method.getOrBuildGraph().iterateExprents(exprent -> {
            List<Exprent> lst = exprent.getAllExprents(true);
            lst.add(exprent);

            for (Exprent expr : lst) {
              if (expr instanceof NewExprent) {
                InvocationExprent constructor = ((NewExprent)expr).getConstructor();

                if (constructor != null && mapVarMasks.containsKey(constructor.getClassname())) { // non-static inner class constructor
                  String refClassName = constructor.getClassname();
                  ClassNode nestedClassNode = node.getClassNode(refClassName);

                  if (nestedClassNode.type != ClassNode.Type.MEMBER) {
                    List<VarFieldPair> mask = mapVarMasks.get(refClassName).get(constructor.getStringDescriptor());

                    if (!mapVarFieldPairs.containsKey(refClassName)) {
                      mapVarFieldPairs.put(refClassName, new HashMap<>());
                    }

                    List<VarFieldPair> lstTemp = new ArrayList<>();

                    for (int i = 0; i < mask.size(); i++) {
                      Exprent param = constructor.getLstParameters().get(i);
                      VarFieldPair pair = null;

                      if (param instanceof VarExprent && mask.get(i) != null) {
                        VarVersionPair varPair = new VarVersionPair((VarExprent)param);

                        // FIXME: flags of variables are wrong! Correct the entire functionality.
                        // if(method.varproc.getVarFinal(varPair) != VarTypeProcessor.VAR_NON_FINAL) {
                        pair = new VarFieldPair(mask.get(i).fieldKey, varPair);
                        // }
                      }

                      lstTemp.add(pair);
                    }

                    List<VarFieldPair> pairMask = mapVarFieldPairs.get(refClassName).get(constructor.getStringDescriptor());
                    if (pairMask == null) {
                      pairMask = lstTemp;
                    }
                    else {
                      for (int i = 0; i < pairMask.size(); i++) {
                        if (!InterpreterUtil.equalObjects(pairMask.get(i), lstTemp.get(i))) {
                          pairMask.set(i, null);
                        }
                      }
                    }

                    mapVarFieldPairs.get(refClassName).put(constructor.getStringDescriptor(), pairMask);
                    // If there was no EnclosingMethod attribute or it was invalid, replace it
                    if (nestedClassNode.enclosingMethod == null || node.getWrapper().getMethods().getWithKey(nestedClassNode.enclosingMethod) == null) {
                      nestedClassNode.enclosingMethod =
                        InterpreterUtil.makeUniqueKey(method.methodStruct.getName(), method.methodStruct.getDescriptor());
                    }
                  }
                }
              }
            }

            return 0;
          });
        }
      }
    }

    // merge var masks
    for (Entry<String, Map<String, List<VarFieldPair>>> enclosing : mapVarMasks.entrySet()) {
      ClassNode nestedNode = node.getClassNode(enclosing.getKey());

      // intersection
      List<VarFieldPair> interPairMask = null;
      // merge referenced constructors
      if (mapVarFieldPairs.containsKey(enclosing.getKey())) {
        for (List<VarFieldPair> mask : mapVarFieldPairs.get(enclosing.getKey()).values()) {
          if (interPairMask == null) {
            interPairMask = new ArrayList<>(mask);
          }
          else {
            mergeListSignatures(interPairMask, mask, false);
          }
        }
      }

      List<VarFieldPair> interMask = null;
      // merge all constructors
      for (List<VarFieldPair> mask : enclosing.getValue().values()) {
        if (interMask == null) {
          interMask = new ArrayList<>(mask);
        }
        else {
          mergeListSignatures(interMask, mask, false);
        }
      }

      if (interPairMask == null) { // member or local and never instantiated
        interPairMask = interMask != null ? new ArrayList<>(interMask) : new ArrayList<>();

        boolean found = false;

        for (int i = 0; i < interPairMask.size(); i++) {
          if (interPairMask.get(i) != null) {
            if (found) {
              interPairMask.set(i, null);
            }
            found = true;
          }
        }
      }

      mergeListSignatures(interPairMask, interMask, true);

      for (VarFieldPair pair : interPairMask) {
        if (pair != null && !pair.fieldKey.isEmpty()) {
          nestedNode.mapFieldsToVars.put(pair.fieldKey, pair.varPair);
        }
      }

      // set resulting constructor signatures
      for (Entry<String, List<VarFieldPair>> entry : enclosing.getValue().entrySet()) {
        mergeListSignatures(entry.getValue(), interPairMask, false);

        List<VarVersionPair> mask = new ArrayList<>(entry.getValue().size());
        for (VarFieldPair pair : entry.getValue()) {
          mask.add(pair != null && !pair.fieldKey.isEmpty() ? pair.varPair : null);
        }
        nestedNode.getWrapper().getMethodWrapper(CodeConstants.INIT_NAME, entry.getKey()).synthParameters = mask;
      }
    }
  }

  private static MethodWrapper findEnclosingMethod(ClassNode child) {
    if (child.enclosingMethod == null) return null;
    if (child.parent != null) {
      MethodWrapper fromParent = child.parent.getWrapper().getMethods().getWithKey(child.enclosingMethod);
      if (fromParent != null) {
        return fromParent;
      }
    }
    for (String enclosingClassName : child.enclosingClasses) {
      ClassNode enclosingClass = DecompilerContext.getClassProcessor().getMapRootClasses().get(enclosingClassName);
      if (enclosingClass != null) {
        MethodWrapper fromEnclosing = enclosingClass.getWrapper().getMethods().getWithKey(child.enclosingMethod);
        if (fromEnclosing != null) {
          return fromEnclosing;
        }
      }
    }
    if (child.type != ClassNode.Type.MEMBER) {
      DecompilerContext.getLogger().writeMessage("Couldn't find enclosing method \"" + child.enclosingMethod + "\" of " + child.classStruct.qualifiedName + " in " + child.enclosingClasses, IFernflowerLogger.Severity.WARN);
    }
    return null;
  }

  private static void insertLocalVars(ClassNode parent, ClassNode child) {
    // enclosing method, is null iff member class
    MethodWrapper enclosingMethod = findEnclosingMethod(child);

    // iterate all child methods
    for (MethodWrapper method : child.getWrapper().getMethods()) {
      if (method.root != null) { // neither abstract nor native
        Map<VarVersionPair, String> mapNewNames = new HashMap<>();  // local var names
        Map<VarVersionPair, VarType> mapNewTypes = new HashMap<>();  // local var types
        Map<VarVersionPair, LocalVariable> mapNewLVTs = new HashMap<>(); // local var table entries

        if (enclosingMethod != null) {
          method.methodStruct.getVariableNamer().addParentContext(enclosingMethod.methodStruct.getVariableNamer());
        }

        final Map<Integer, VarVersionPair> mapParamsToNewVars = new HashMap<>();
        if (method.synthParameters != null) {
          int index = 0, varIndex = 1;
          MethodDescriptor md = MethodDescriptor.parseDescriptor(method.methodStruct.getDescriptor());

          for (VarVersionPair pair : method.synthParameters) {
            if (pair != null) {
              VarVersionPair newVar = new VarVersionPair(method.counter.getCounterAndIncrement(CounterContainer.VAR_COUNTER), 0);

              mapParamsToNewVars.put(varIndex, newVar);

              String varName = null;
              VarType varType = null;
              LocalVariable varLVT = null;

              if (child.type != ClassNode.Type.MEMBER) {
                varName = enclosingMethod.varproc.getVarName(pair);
                varType = enclosingMethod.varproc.getVarType(pair);
                varLVT = enclosingMethod.varproc.getVarLVT(pair);

                enclosingMethod.varproc.setVarFinal(pair, FinalType.EXPLICIT_FINAL);
              }

              if (pair.var == -1 || "this".equals(varName) || (varLVT != null && "this".equals(varLVT.getName()))) {
                if (parent.simpleName == null) {
                  // anonymous enclosing class, no access to this
                  varName = VarExprent.VAR_NAMELESS_ENCLOSURE;
                }
                else {
                  varName = parent.simpleName + ".this";
                }
                if (varLVT != null) {
                  varLVT = varLVT.rename(varName);
                }
                method.varproc.getThisVars().put(newVar, parent.classStruct.qualifiedName);
              }

              mapNewNames.put(newVar, varName);
              mapNewTypes.put(newVar, varType);
              mapNewLVTs.put(newVar, varLVT);
            }

            varIndex += md.params[index++].stackSize;
          }
        }

        Map<String, VarVersionPair> mapFieldsToNewVars = new HashMap<>();
        for (ClassNode classNode = child; classNode != null; classNode = classNode.parent) {
          for (Entry<String, VarVersionPair> entry : classNode.mapFieldsToVars.entrySet()) {
            VarVersionPair newVar = new VarVersionPair(method.counter.getCounterAndIncrement(CounterContainer.VAR_COUNTER), 0);

            mapFieldsToNewVars.put(InterpreterUtil.makeUniqueKey(classNode.classStruct.qualifiedName, entry.getKey()), newVar);

            String varName = null;
            VarType varType = null;
            LocalVariable varLVT = null;

            if (classNode.type != ClassNode.Type.MEMBER) {
              MethodWrapper enclosing_method = findEnclosingMethod(classNode);

              varName = enclosing_method.varproc.getVarName(entry.getValue());
              varType = enclosing_method.varproc.getVarType(entry.getValue());
              varLVT = enclosing_method.varproc.getVarLVT(entry.getValue());

              enclosing_method.varproc.setVarFinal(entry.getValue(), FinalType.EXPLICIT_FINAL);
            }

            if (entry.getValue().var == -1 || "this".equals(varName) || (varLVT != null && "this".equals(varLVT.getName()))) {
              if (classNode.parent.simpleName == null) {
                // anonymous enclosing class, no access to this
                varName = VarExprent.VAR_NAMELESS_ENCLOSURE;
              }
              else {
                varName = classNode.parent.simpleName + ".this";
              }
              if (varLVT != null) {
                varLVT = varLVT.rename(varName);
              }
              method.varproc.getThisVars().put(newVar, classNode.parent.classStruct.qualifiedName);
            }

            mapNewNames.put(newVar, varName);
            mapNewTypes.put(newVar, varType);
            mapNewLVTs.put(newVar, varLVT);

            // hide synthetic field
            if (classNode == child) { // fields higher up the chain were already handled with their classes
              StructField fd = child.classStruct.getFields().getWithKey(entry.getKey());
              child.getWrapper().getHiddenMembers().add(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()));
            }
          }
        }

        Set<String> setNewOuterNames = new HashSet<>(mapNewNames.values());
        setNewOuterNames.removeAll(method.setOuterVarNames);

        method.varproc.refreshVarNames(new VarNamesCollector(setNewOuterNames));
        method.setOuterVarNames.addAll(setNewOuterNames);

        for (Entry<VarVersionPair, String> entry : mapNewNames.entrySet()) {
          VarVersionPair pair = entry.getKey();
          VarType type = mapNewTypes.get(pair);
          LocalVariable lvt = mapNewLVTs.get(pair);

          method.varproc.setInheritedName(pair, entry.getValue());
          if (type != null) {
            method.varproc.setVarType(pair, type);
          }
          if (lvt != null) {
            method.varproc.setVarLVT(pair, lvt);
          }
        }

        iterateExprents(method.getOrBuildGraph(), new ExprentIteratorWithReplace() {
          @Override
          public Exprent processExprent(Exprent exprent) {
            if (exprent instanceof AssignmentExprent) {
              AssignmentExprent assignExpr = (AssignmentExprent)exprent;
              if (assignExpr.getLeft() instanceof FieldExprent) {
                FieldExprent fExpr = (FieldExprent)assignExpr.getLeft();
                String qName = child.classStruct.qualifiedName;
                if (fExpr.getClassname().equals(qName) &&  // process this class only
                    mapFieldsToNewVars.containsKey(InterpreterUtil.makeUniqueKey(qName, fExpr.getName(), fExpr.getDescriptor().descriptorString))) {
                  return null;
                }
              }
            }

            if (child.type == ClassNode.Type.ANONYMOUS &&
                CodeConstants.INIT_NAME.equals(method.methodStruct.getName()) &&
                exprent instanceof InvocationExprent) {
              InvocationExprent invokeExpr = (InvocationExprent)exprent;
              if (invokeExpr.getFunctype() == InvocationExprent.Type.INIT) {
                // invocation of the super constructor in an anonymous class
                child.superInvocation = invokeExpr; // FIXME: save original names of parameters
                return null;
              }
            }

            Exprent ret = replaceExprent(exprent);

            return ret == null ? exprent : ret;
          }

          private Exprent replaceExprent(Exprent exprent) {
            if (exprent instanceof VarExprent) {
              int varIndex = ((VarExprent)exprent).getIndex();
              if (mapParamsToNewVars.containsKey(varIndex)) {
                VarVersionPair newVar = mapParamsToNewVars.get(varIndex);
                method.varproc.getExternalVars().add(newVar);
                VarExprent ret = new VarExprent(newVar.var, method.varproc.getVarType(newVar), method.varproc, exprent.bytecode);
                LocalVariable lvt = method.varproc.getVarLVT(newVar);
                if (lvt != null) {
                  ret.setLVT(lvt);
                }
                return ret;
              }
            }
            else if (exprent instanceof FieldExprent) {
              FieldExprent fExpr = (FieldExprent)exprent;
              String key = InterpreterUtil.makeUniqueKey(fExpr.getClassname(), fExpr.getName(), fExpr.getDescriptor().descriptorString);
              if (mapFieldsToNewVars.containsKey(key)) {
                //if(fExpr.getClassname().equals(child.classStruct.qualifiedName) &&
                //        mapFieldsToNewVars.containsKey(key)) {
                VarVersionPair newVar = mapFieldsToNewVars.get(key);
                method.varproc.getExternalVars().add(newVar);
                VarExprent ret = new VarExprent(newVar.var, method.varproc.getVarType(newVar), method.varproc, exprent.bytecode);
                LocalVariable lvt = method.varproc.getVarLVT(newVar);
                if (lvt != null) {
                  ret.setLVT(lvt);
                }
                return ret;
              }
            }

            boolean replaced = true;
            while (replaced) {
              replaced = false;

              for (Exprent expr : exprent.getAllExprents()) {
                Exprent retExpr = replaceExprent(expr);
                if (retExpr != null) {
                  exprent.replaceExprent(expr, retExpr);
                  replaced = true;
                  break;
                }
              }
            }

            return null;
          }
        });
      }
    }
  }

  private static Map<String, List<VarFieldPair>> getMaskLocalVars(ClassWrapper wrapper) {
    Map<String, List<VarFieldPair>> mapMasks = new HashMap<>();

    StructClass cl = wrapper.getClassStruct();

    // iterate over constructors
    for (StructMethod mt : cl.getMethods()) {
      if (CodeConstants.INIT_NAME.equals(mt.getName())) {
        MethodDescriptor md = MethodDescriptor.parseDescriptor(mt.getDescriptor());
        MethodWrapper method = wrapper.getMethodWrapper(CodeConstants.INIT_NAME, mt.getDescriptor());
        DirectGraph graph = method.getOrBuildGraph();

        if (graph != null) { // something gone wrong, should not be null
          List<VarFieldPair> fields = new ArrayList<>(md.params.length);

          int varIndex = 1;
          for (int i = 0; i < md.params.length; i++) {  // no static methods allowed
            String keyField = getEnclosingVarField(cl, method, graph, varIndex);
            fields.add(keyField == null ? null : new VarFieldPair(keyField, new VarVersionPair(-1, 0))); // TODO: null?
            varIndex += md.params[i].stackSize;
          }

          mapMasks.put(mt.getDescriptor(), fields);
        }
      }
    }

    return mapMasks;
  }

  private static String getEnclosingVarField(StructClass cl, MethodWrapper method, DirectGraph graph, int index) {
    String field = "";

    // parameter variable final
    if (method.varproc.getVarFinal(new VarVersionPair(index, 0)) == FinalType.NON_FINAL) {
      return null;
    }

    boolean noSynthFlag = DecompilerContext.getOption(IFernflowerPreferences.SYNTHETIC_NOT_SET);

    // no loop at the begin
    DirectNode firstNode = graph.first;
    if (firstNode.preds().isEmpty()) {
      // assignment to a synthetic field?
      for (Exprent exprent : firstNode.exprents) {
        if (exprent instanceof AssignmentExprent) {
          AssignmentExprent assignExpr = (AssignmentExprent)exprent;
          if (assignExpr.getRight() instanceof VarExprent &&
              ((VarExprent)assignExpr.getRight()).getIndex() == index &&
              assignExpr.getLeft() instanceof FieldExprent) {
            FieldExprent left = (FieldExprent)assignExpr.getLeft();
            StructField fd = cl.getField(left.getName(), left.getDescriptor().descriptorString);
            if (fd != null &&
                cl.qualifiedName.equals(left.getClassname()) &&
                (fd.isSynthetic() || noSynthFlag && possiblySyntheticField(fd))) {
              // local (== not inherited) field
              field = InterpreterUtil.makeUniqueKey(left.getName(), left.getDescriptor().descriptorString);
              break;
            }
          }
        }
      }
    }

    return field;
  }

  private static boolean possiblySyntheticField(StructField fd) {
    return fd.getName().contains("$") && fd.hasModifier(CodeConstants.ACC_FINAL) && fd.hasModifier(CodeConstants.ACC_PRIVATE);
  }

  private static void mergeListSignatures(List<VarFieldPair> first, List<VarFieldPair> second, boolean both) {
    int i = 1;

    while (first.size() > i && second.size() > i) {
      VarFieldPair fObj = first.get(first.size() - i);
      VarFieldPair sObj = second.get(second.size() - i);

      if (!isEqual(both, fObj, sObj)) {
        first.set(first.size() - i, null);
        if (both) {
          second.set(second.size() - i, null);
        }
      }
      else if (fObj != null) {
        if (fObj.varPair.var == -1) {
          fObj.varPair = sObj.varPair;
        }
        else {
          sObj.varPair = fObj.varPair;
        }
      }

      i++;
    }

    for (int j = 1; j <= first.size() - i; j++) {
      first.set(j, null);
    }

    if (both) {
      for (int j = 1; j <= second.size() - i; j++) {
        second.set(j, null);
      }
    }

    // first
    if (first.isEmpty()) {
      if (!second.isEmpty() && both) {
        second.set(0, null);
      }
    }
    else if (second.isEmpty()) {
      first.set(0, null);
    }
    else {
      VarFieldPair fObj = first.get(0);
      VarFieldPair sObj = second.get(0);

      if (!isEqual(both, fObj, sObj)) {
        first.set(0, null);
        if (both) {
          second.set(0, null);
        }
      }
      else if (fObj != null) {
        if (fObj.varPair.var == -1) {
          fObj.varPair = sObj.varPair;
        }
        else {
          sObj.varPair = fObj.varPair;
        }
      }
    }
  }

  private static boolean isEqual(boolean both, VarFieldPair fObj, VarFieldPair sObj) {
    boolean eq;
    if (fObj == null || sObj == null) {
      eq = (fObj == sObj);
    }
    else {
      eq = true;
      if (fObj.fieldKey.length() == 0) {
        fObj.fieldKey = sObj.fieldKey;
      }
      else if (sObj.fieldKey.length() == 0) {
        if (both) {
          sObj.fieldKey = fObj.fieldKey;
        }
      }
      else {
        eq = fObj.fieldKey.equals(sObj.fieldKey);
      }
    }
    return eq;
  }

  private static void setLocalClassDefinition(MethodWrapper method, ClassNode node) {
    RootStatement root = method.root;

    if (root == null) {
      method.addComment("$VF: Couldn't set local class definition as local class statement was null");
      method.addErrorComment = true;

      return;
    }

    Set<Statement> setStats = new HashSet<>();
    VarType classType = new VarType(node.classStruct.qualifiedName, true);

    Statement statement = getDefStatement(root, classType, setStats);
    if (statement == null) {
      // unreferenced local class
      statement = root.getFirst();
    }

    Statement first = findFirstBlock(statement, setStats);

    List<Exprent> lst;
    if (first == null) {
      lst = statement.getVarDefinitions();
    }
    else if (first.getExprents() == null) {
      lst = first.getVarDefinitions();
    }
    else {
      lst = first.getExprents();
    }

    int addIndex = 0;
    for (Exprent expr : lst) {
      if (searchForClass(expr, classType)) {
        break;
      }
      addIndex++;
    }

    VarExprent var = new VarExprent(method.counter.getCounterAndIncrement(CounterContainer.VAR_COUNTER), classType, method.varproc);
    var.setDefinition(true);
    var.setClassDef(true);

    lst.add(addIndex, var);
  }

  private static Statement findFirstBlock(Statement stat, Set<Statement> setStats) {
    LinkedList<Statement> stack = new LinkedList<>();
    stack.add(stat);

    while (!stack.isEmpty()) {
      Statement st = stack.remove(0);

      if (stack.isEmpty() || setStats.contains(st)) {
        if (st.isLabeled() && !stack.isEmpty() || st.getExprents() != null) {
          return st;
        }

        stack.clear();

        switch (st.type) {
          case SEQUENCE:
            stack.addAll(0, st.getStats());
            break;
          case IF:
          case ROOT:
          case SWITCH:
          case SYNCHRONIZED:
            stack.add(st.getFirst());
            break;
          default:
            return st;
        }
      }
    }

    return null;
  }

  private static Statement getDefStatement(Statement stat, VarType classType, Set<? super Statement> setStats) {
    List<Exprent> lst = new ArrayList<>();
    Statement retStat = null;

    if (stat.getExprents() == null) {
      int counter = 0;

      for (Object obj : stat.getSequentialObjects()) {
        if (obj instanceof Statement) {
          Statement st = (Statement)obj;

          Statement stTemp = getDefStatement(st, classType, setStats);

          if (stTemp != null) {
            if (counter == 1) {
              retStat = stat;
              break;
            }
            retStat = stTemp;
            counter++;
          }

          if (st instanceof DoStatement) {
            DoStatement dost = (DoStatement)st;

            lst.addAll(dost.getInitExprentList());
            lst.addAll(dost.getConditionExprentList());
          }
        }
        else if (obj instanceof Exprent) {
          lst.add((Exprent)obj);
        }
      }
    }
    else {
      lst = stat.getExprents();
    }

    if (retStat != stat) {
      for (Exprent exprent : lst) {
        if (exprent != null && searchForClass(exprent, classType)) {
          retStat = stat;
          break;
        }
      }
    }

    if (retStat != null) {
      setStats.add(stat);
    }

    return retStat;
  }

  private static boolean searchForClass(Exprent exprent, VarType classType) {
    List<Exprent> lst = exprent.getAllExprents(true);
    lst.add(exprent);

    String classname = classType.value;

    for (Exprent expr : lst) {
      boolean res = false;

      switch (expr.type) {
        case CONST:
          ConstExprent constExpr = (ConstExprent)expr;
          res = (VarType.VARTYPE_CLASS.equals(constExpr.getConstType()) && classname.equals(constExpr.getValue()) ||
                 classType.equals(constExpr.getConstType()));
          break;
        case FIELD:
          res = classname.equals(((FieldExprent)expr).getClassname());
          break;
        case INVOCATION:
          res = containsType(((InvocationExprent) expr), classType);
          break;
        case NEW:
          NewExprent newExpr = (NewExprent) expr;
          VarType newType = newExpr.getNewType();
          res = newType.type == CodeConstants.TYPE_OBJECT && classname.equals(newType.value) || containsType(newExpr.getConstructor(), classType);
          break;
        case VAR:
          VarExprent varExpr = (VarExprent)expr;
          if (varExpr.isDefinition()) {
            res = containsType(varExpr.getInferredExprType(null), classType);
          }
          break;
      }

      if (res) {
        return true;
      }
    }

    return false;
  }

  private static boolean containsType(InvocationExprent haystack, VarType needle) {
    if (haystack == null) return false;
    if (needle.value.equals(haystack.getClassname())) return true;
    List<PooledConstant> bootstrapArgs = haystack.getBootstrapArguments();
    if (bootstrapArgs == null) return false;
    for (PooledConstant bootstrapArg : bootstrapArgs) {
      if (bootstrapArg instanceof LinkConstant && needle.value.equals(((LinkConstant) bootstrapArg).classname)) {
        return true;
      }
    }
    return false;
  }

  private static boolean containsType(VarType haystack, VarType needle) {
    if (haystack == null || needle == null) return false;
    if (needle.equals(haystack) || (haystack.arrayDim > 0 && haystack.value.equals(needle.value))) {
      return true;
    }
    if (haystack.isGeneric()) {
      for (VarType arg : ((GenericType) haystack).getArguments()) {
        if (containsType(arg, needle)) return true;
      }
    }
    return false;
  }

  private static class VarFieldPair {
    public String fieldKey;
    public VarVersionPair varPair;

    VarFieldPair(String field, VarVersionPair varPair) {
      this.fieldKey = field;
      this.varPair = varPair;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) return true;
      if (!(o instanceof VarFieldPair)) return false;

      VarFieldPair pair = (VarFieldPair)o;
      return fieldKey.equals(pair.fieldKey) && varPair.equals(pair.varPair);
    }

    @Override
    public int hashCode() {
      return fieldKey.hashCode() + varPair.hashCode();
    }
  }

  private static interface ExprentIteratorWithReplace {
    // null - remove exprent
    // ret != exprent - replace exprent with ret
    Exprent processExprent(Exprent exprent);
  }

  private static void iterateExprents(DirectGraph graph, ExprentIteratorWithReplace iter) {
    LinkedList<DirectNode> stack = new LinkedList<DirectNode>();
    stack.add(graph.first);

    HashSet<DirectNode> setVisited = new HashSet<DirectNode>();

    while (!stack.isEmpty()) {

      DirectNode node = stack.removeFirst();

      if (setVisited.contains(node)) {
        continue;
      }
      setVisited.add(node);

      for (int i = 0; i < node.exprents.size(); i++) {
        Exprent res = iter.processExprent(node.exprents.get(i));

        if (res == null) {
          node.exprents.remove(i);
          i--;
        }
        else if (res != node.exprents.get(i)) {
          node.exprents.set(i, res);
        }
      }

      stack.addAll(node.succs());
    }
  }
}
