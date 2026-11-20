package org.test

import lib.*

fun upcastInterface(javaInterface: JavaInterface): LibraryInterface = javaInterface

fun upcastClass(javaClass: JavaClass): LibraryClass = javaClass

fun upcastNestedInterface(javaInterface: JavaInterfaceExtendingNested): LibraryOuterClass.NestedInterface = javaInterface

fun interfaceMembers(javaInterface: JavaInterface): Int = javaInterface.interfaceFunction() + javaInterface.extensionFunction()

fun classMembers(javaClass: JavaClass): Int =
    javaClass.classFunction() + javaClass.classProperty.length + javaClass.interfaceFunction() + javaClass.extensionFunction()

fun typeArgument(javaInterface: JavaGenericInterface): String = javaInterface.get()
