// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.java.decompiler.main;

import org.jetbrains.java.decompiler.main.collectors.BytecodeSourceMapper;
import org.jetbrains.java.decompiler.main.collectors.CounterContainer;
import org.jetbrains.java.decompiler.main.collectors.ImportCollector;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarProcessor;
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory;
import org.jetbrains.java.decompiler.modules.renamer.PoolInterceptor;
import org.jetbrains.java.decompiler.struct.StructContext;

import java.util.Map;
import java.util.Objects;

public class DecompilerContext {
  public static final String CURRENT_CLASS = "CURRENT_CLASS";
  public static final String CURRENT_CLASS_WRAPPER = "CURRENT_CLASS_WRAPPER";
  public static final String CURRENT_CLASS_NODE = "CURRENT_CLASS_NODE";
  public static final String CURRENT_METHOD_WRAPPER = "CURRENT_METHOD_WRAPPER";
  public static final String CURRENT_VAR_PROCESSOR = "CURRENT_VAR_PROCESSOR";
  public static final String RENAMER_FACTORY = "RENAMER_FACTORY";

  public final Map<String, Object> properties;
  public final IFernflowerLogger logger;
  public final StructContext structContext;
  public final ClassesProcessor classProcessor;
  public final PoolInterceptor poolInterceptor;
  public final IVariableNamingFactory renamerFactory;
  private ImportCollector importCollector;
  private VarProcessor varProcessor;
  private CounterContainer counterContainer;
  private BytecodeSourceMapper bytecodeSourceMapper;

  public DecompilerContext(Map<String, Object> properties,
                           IFernflowerLogger logger,
                           StructContext structContext,
                           ClassesProcessor classProcessor,
                           PoolInterceptor interceptor,
                           IVariableNamingFactory renamerFactory) {
    Objects.requireNonNull(properties);
    Objects.requireNonNull(logger);
    Objects.requireNonNull(structContext);
    Objects.requireNonNull(classProcessor);

    this.properties = properties;
    this.logger = logger;
    this.structContext = structContext;
    this.classProcessor = classProcessor;
    this.poolInterceptor = interceptor;
    this.renamerFactory = renamerFactory;
    this.counterContainer = new CounterContainer();
  }

  // *****************************************************************************
  // context setup and update
  // *****************************************************************************

  private static final ThreadLocal<DecompilerContext> currentContext = new ThreadLocal<>();

  public static DecompilerContext getCurrentContext() {
    return currentContext.get();
  }

  public static void setCurrentContext(DecompilerContext context) {
    if (context == null) {
      currentContext.remove();
    } else {
      currentContext.set(context);
    }
  }

  public static void setProperty(String key, Object value) {
    getCurrentContext().properties.put(key, value);
  }

  public static void startClass(ImportCollector importCollector) {
    DecompilerContext context = getCurrentContext();
    context.importCollector = importCollector;
    context.counterContainer = new CounterContainer();
    context.bytecodeSourceMapper = new BytecodeSourceMapper();
  }

  public static void startMethod(VarProcessor varProcessor) {
    DecompilerContext context = getCurrentContext();
    context.varProcessor = varProcessor;
    context.counterContainer = new CounterContainer();
  }

  // *****************************************************************************
  // context access
  // *****************************************************************************

  public static Object getProperty(String key) {
    return getCurrentContext().properties.get(key);
  }

  public static boolean getOption(String key) {
    return "1".equals(getProperty(key));
  }

  public static int getIntOption(String key) {
    try {
      return Integer.parseInt((String) getProperty(key));
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  public static String getNewLineSeparator() {
    return getOption(IFernflowerPreferences.NEW_LINE_SEPARATOR) ?
           IFernflowerPreferences.LINE_SEPARATOR_UNX : IFernflowerPreferences.LINE_SEPARATOR_WIN;
  }

  public static IFernflowerLogger getLogger() {
    return getCurrentContext().logger;
  }

  public static StructContext getStructContext() {
    return getCurrentContext().structContext;
  }

  public static ClassesProcessor getClassProcessor() {
    return getCurrentContext().classProcessor;
  }

  public static PoolInterceptor getPoolInterceptor() {
    return getCurrentContext().poolInterceptor;
  }

  public static IVariableNamingFactory getNamingFactory() {
    return getCurrentContext().renamerFactory;
  }

  public static ImportCollector getImportCollector() {
    return getCurrentContext().importCollector;
  }

  public static VarProcessor getVarProcessor() {
    return getCurrentContext().varProcessor;
  }

  public static CounterContainer getCounterContainer() {
    return getCurrentContext().counterContainer;
  }

  public static BytecodeSourceMapper getBytecodeSourceMapper() {
    return getCurrentContext().bytecodeSourceMapper;
  }
}
