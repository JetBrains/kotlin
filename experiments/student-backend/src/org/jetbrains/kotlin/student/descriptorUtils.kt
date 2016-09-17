package org.jetbrains.kotlin.student.descriptorUtils

import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.addToStdlib.check

/**
 * Created by minamoto on 17/09/2016.
 * this code is copied from org.jetbrains.kotlin.js.descriptorUtils
 */
val KotlinType.nameIfStandardType: Name?
    get() {
        return constructor.declarationDescriptor
                ?.check { descriptor ->
                    descriptor.builtIns.isBuiltInPackageFragment(descriptor.containingDeclaration as? PackageFragmentDescriptor)
                }
                ?.name
    }
