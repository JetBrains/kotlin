package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.Name

private val STDLIB_MODULE_NAME = Name.special("<$KONAN_STDLIB_NAME>")

fun ModuleDescriptor.isKonanStdlib() = name == STDLIB_MODULE_NAME
