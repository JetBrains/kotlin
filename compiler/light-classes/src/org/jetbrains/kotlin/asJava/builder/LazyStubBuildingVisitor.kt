/////*
//// * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
//// * that can be found in the license/LICENSE.txt file.
//// */
////
//package org.jetbrains.kotlin.asJava.builder
//
//import com.intellij.psi.CommonClassNames
//import com.intellij.psi.impl.cache.TypeInfo
//import com.intellij.psi.impl.compiled.InnerClassSourceStrategy
//import com.intellij.psi.impl.compiled.StubBuildingVisitor
//import com.intellij.psi.impl.java.stubs.JavaStubElementTypes
//import com.intellij.psi.impl.java.stubs.PsiClassStub
//import com.intellij.psi.impl.java.stubs.impl.*
//import com.intellij.psi.stubs.StubElement
//import com.intellij.util.ArrayUtil
//import com.intellij.util.BitUtil.isSet
//import com.intellij.util.cls.ClsFormatException
//import org.jetbrains.org.objectweb.asm.MethodVisitor
//import org.jetbrains.org.objectweb.asm.Opcodes
//
//class LazyStubBuildingVisitor<T>(
//    classSource: T,
//    innersStrategy: InnerClassSourceStrategy<T>?,
//    parent: StubElement<*>?,
//    access: Int,
//    shortName: String?
//) : StubBuildingVisitor<T>(classSource, innersStrategy, parent, access, shortName) {
//
//    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
//
//    }
//}
