// Copyright 2019 FabricMC project. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package net.fabricmc.fernflower.api;

import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.StructField;
import org.jetbrains.java.decompiler.struct.StructMethod;

import java.util.Map;

/**
 * Provides (optional) javadoc for Classes/Methods/Fields encountered by
 *  {@link org.jetbrains.java.decompiler.main.ClassWriter}.
 *
 * May be set as a property in the constructor of {@link org.jetbrains.java.decompiler.main.Fernflower} by using
 *  the key {@code IFabricJavadocProvider.PROPERTY_NAME}
 */
public interface IFabricJavadocProvider {
  String PROPERTY_NAME = "fabric:javadoc";

  String getClassDoc(StructClass structClass);

  String getFieldDoc(StructClass structClass, StructField structField);

  String getMethodDoc(StructClass structClass, StructMethod structMethod);
}