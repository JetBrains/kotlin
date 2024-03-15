// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main;

import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.ClassesProcessor.ClassNode;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.main.rels.ClassWrapper;
import org.jetbrains.java.decompiler.main.rels.MethodWrapper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.*;
import org.jetbrains.java.decompiler.modules.decompiler.flow.DirectGraph;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FunctionExprent.FunctionType;
import org.jetbrains.java.decompiler.modules.decompiler.stats.BasicBlockStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.CatchStatement;
import org.jetbrains.java.decompiler.modules.decompiler.stats.RootStatement;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.gen.MethodDescriptor;
import org.jetbrains.java.decompiler.struct.gen.VarType;
import org.jetbrains.java.decompiler.util.InterpreterUtil;
import org.jetbrains.java.decompiler.util.VBStyleCollection;

import java.util.*;
import java.util.Map.Entry;

public final class ClassReference14Processor {
  private static final ExitExprent BODY_EXPR;
  private static final ExitExprent HANDLER_EXPR;

  static {
    InvocationExprent invFor = new InvocationExprent();
    invFor.setName("forName");
    invFor.setClassname("java/lang/Class");
    invFor.setStringDescriptor("(Ljava/lang/String;)Ljava/lang/Class;");
    invFor.setDescriptor(MethodDescriptor.parseDescriptor("(Ljava/lang/String;)Ljava/lang/Class;"));
    invFor.setStatic(true);
    invFor.setLstParameters(Collections.singletonList(new VarExprent(0, VarType.VARTYPE_STRING, null)));
    BODY_EXPR = new ExitExprent(ExitExprent.Type.RETURN, invFor, VarType.VARTYPE_CLASS, null, null);

    InvocationExprent ctor = new InvocationExprent();
    ctor.setName(CodeConstants.INIT_NAME);
    ctor.setClassname("java/lang/NoClassDefFoundError");
    ctor.setStringDescriptor("()V");
    ctor.setFunctype(InvocationExprent.Type.INIT);
    ctor.setDescriptor(MethodDescriptor.parseDescriptor("()V"));
    NewExprent newExpr = new NewExprent(new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/NoClassDefFoundError"), new ArrayList<>(), null);
    newExpr.setConstructor(ctor);
    InvocationExprent invCause = new InvocationExprent();
    invCause.setName("initCause");
    invCause.setClassname("java/lang/NoClassDefFoundError");
    invCause.setStringDescriptor("(Ljava/lang/Throwable;)Ljava/lang/Throwable;");
    invCause.setDescriptor(MethodDescriptor.parseDescriptor("(Ljava/lang/Throwable;)Ljava/lang/Throwable;"));
    invCause.setInstance(newExpr);
    invCause.setLstParameters(
      Collections.singletonList(new VarExprent(2, new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/ClassNotFoundException"), null)));
    HANDLER_EXPR = new ExitExprent(ExitExprent.Type.THROW, invCause, null, null, null);
  }

  public static void processClassReferences(ClassNode node) {
    // find the synthetic method Class class$(String) if present
    Map<ClassWrapper, MethodWrapper> mapClassMeths = new HashMap<>();
    mapClassMethods(node, mapClassMeths);
    if (mapClassMeths.isEmpty()) {
      return;
    }

    Set<ClassWrapper> setFound = new HashSet<>();
    processClassRec(node, mapClassMeths, setFound);

    if (!setFound.isEmpty()) {
      for (ClassWrapper wrp : setFound) {
        StructMethod mt = mapClassMeths.get(wrp).methodStruct;
        wrp.getHiddenMembers().add(InterpreterUtil.makeUniqueKey(mt.getName(), mt.getDescriptor()));
      }
    }
  }

  private static void processClassRec(ClassNode node, Map<ClassWrapper, MethodWrapper> mapClassMeths, Set<? super ClassWrapper> setFound) {
    ClassWrapper wrapper = node.getWrapper();

    // search code
    for (MethodWrapper meth : wrapper.getMethods()) {
      RootStatement root = meth.root;
      if (root != null) {
        DirectGraph graph = meth.getOrBuildGraph();
        graph.iterateExprents(exprent -> {
          for (Entry<ClassWrapper, MethodWrapper> ent : mapClassMeths.entrySet()) {
            if (replaceInvocations(exprent, ent.getKey(), ent.getValue())) {
              setFound.add(ent.getKey());
            }
          }
          return 0;
        });
      }
    }

    // search initializers
    for (int j = 0; j < 2; j++) {
      VBStyleCollection<Exprent, String> initializers =
        j == 0 ? wrapper.getStaticFieldInitializers() : wrapper.getDynamicFieldInitializers();

      for (int i = 0; i < initializers.size(); i++) {
        for (Entry<ClassWrapper, MethodWrapper> ent : mapClassMeths.entrySet()) {
          Exprent exprent = initializers.get(i);
          if (replaceInvocations(exprent, ent.getKey(), ent.getValue())) {
            setFound.add(ent.getKey());
          }

          String cl = isClass14Invocation(exprent, ent.getKey(), ent.getValue());
          if (cl != null) {
            initializers.set(i, new ConstExprent(VarType.VARTYPE_CLASS, cl.replace('.', '/'), exprent.bytecode));
            setFound.add(ent.getKey());
          }
        }
      }
    }

    // iterate nested classes
    for (ClassNode nd : node.nested) {
      processClassRec(nd, mapClassMeths, setFound);
    }
  }

  private static void mapClassMethods(ClassNode node, Map<ClassWrapper, MethodWrapper> map) {
    boolean noSynthFlag = DecompilerContext.getOption(IFernflowerPreferences.SYNTHETIC_NOT_SET);

    ClassWrapper wrapper = node.getWrapper();

    for (MethodWrapper method : wrapper.getMethods()) {
      StructMethod mt = method.methodStruct;

      if ((noSynthFlag || mt.isSynthetic()) &&
          mt.getDescriptor().equals("(Ljava/lang/String;)Ljava/lang/Class;") &&
          mt.hasModifier(CodeConstants.ACC_STATIC)) {

        RootStatement root = method.root;
        if (root != null && root.getFirst() instanceof CatchStatement) {
          CatchStatement cst = (CatchStatement)root.getFirst();
          if (cst.getStats().size() == 2 && cst.getFirst() instanceof BasicBlockStatement &&
              cst.getStats().get(1) instanceof BasicBlockStatement &&
              cst.getVars().get(0).getVarType().equals(new VarType(CodeConstants.TYPE_OBJECT, 0, "java/lang/ClassNotFoundException"))) {

            BasicBlockStatement body = (BasicBlockStatement)cst.getFirst();
            BasicBlockStatement handler = (BasicBlockStatement)cst.getStats().get(1);

            if (body.getExprents().size() == 1 && handler.getExprents().size() == 1) {
              if (BODY_EXPR.equals(body.getExprents().get(0)) &&
                  HANDLER_EXPR.equals(handler.getExprents().get(0))) {
                map.put(wrapper, method);
                break;
              }
            }
          }
        }
      }
    }

    // iterate nested classes
    for (ClassNode nd : node.nested) {
      mapClassMethods(nd, map);
    }
  }

  private static boolean replaceInvocations(Exprent exprent, ClassWrapper wrapper, MethodWrapper meth) {
    boolean res = false;

    while (true) {
      boolean found = false;

      for (Exprent expr : exprent.getAllExprents()) {
        String cl = isClass14Invocation(expr, wrapper, meth);
        if (cl != null) {
          exprent.replaceExprent(expr, new ConstExprent(VarType.VARTYPE_CLASS, cl.replace('.', '/'), expr.bytecode));
          found = true;
          res = true;
          break;
        }

        res |= replaceInvocations(expr, wrapper, meth);
      }

      if (!found) {
        break;
      }
    }

    return res;
  }

  private static String isClass14Invocation(Exprent exprent, ClassWrapper wrapper, MethodWrapper meth) {
    if (exprent instanceof FunctionExprent) {
      FunctionExprent fexpr = (FunctionExprent)exprent;
      if (fexpr.getFuncType() == FunctionType.TERNARY) {
        if (fexpr.getLstOperands().get(0) instanceof FunctionExprent) {
          FunctionExprent headexpr = (FunctionExprent)fexpr.getLstOperands().get(0);
          if (headexpr.getFuncType() == FunctionType.EQ) {
            if (headexpr.getLstOperands().get(0) instanceof FieldExprent &&
                headexpr.getLstOperands().get(1) instanceof ConstExprent &&
                ((ConstExprent)headexpr.getLstOperands().get(1)).getConstType().equals(VarType.VARTYPE_NULL)) {

              FieldExprent field = (FieldExprent)headexpr.getLstOperands().get(0);
              ClassNode fieldnode = DecompilerContext.getClassProcessor().getMapRootClasses().get(field.getClassname());

              if (fieldnode != null && fieldnode.classStruct.qualifiedName.equals(wrapper.getClassStruct().qualifiedName)) { // source class
                StructField fd =
                  wrapper.getClassStruct().getField(field.getName(), field.getDescriptor().descriptorString);  // FIXME: can be null! why??

                if (fd != null && fd.hasModifier(CodeConstants.ACC_STATIC) &&
                    (fd.isSynthetic() || DecompilerContext.getOption(IFernflowerPreferences.SYNTHETIC_NOT_SET))) {

                  if (fexpr.getLstOperands().get(1) instanceof AssignmentExprent && fexpr.getLstOperands().get(2).equals(field)) {
                    AssignmentExprent asexpr = (AssignmentExprent)fexpr.getLstOperands().get(1);

                    if (asexpr.getLeft().equals(field) && asexpr.getRight() instanceof InvocationExprent) {
                      InvocationExprent invexpr = (InvocationExprent)asexpr.getRight();

                      if (invexpr.getClassname().equals(wrapper.getClassStruct().qualifiedName) &&
                          invexpr.getName().equals(meth.methodStruct.getName()) &&
                          invexpr.getStringDescriptor().equals(meth.methodStruct.getDescriptor())) {

                        if (invexpr.getLstParameters().get(0) instanceof ConstExprent) {
                          wrapper.getHiddenMembers()
                            .add(InterpreterUtil.makeUniqueKey(fd.getName(), fd.getDescriptor()));  // hide synthetic field
                          return ((ConstExprent)invexpr.getLstParameters().get(0)).getValue().toString();
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
    }

    return null;
  }
}
