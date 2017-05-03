package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

internal class UnitProcessor (val context: KonanBackendContext):FileLoweringPass {
    val fqNameTestAnnotation = FqName("org.junit.Test")
    val fqNameBeforeAnnotation = FqName("org.junit.Before")
    val fqNameAfterAnnotation = FqName("org.junit.After")
    val fqNameBeforeClassAnnotation = FqName("org.junit.BeforeClass")
    val fqNameAfterClassAnnotation = FqName("org.junit.AfterClass")

    override fun lower(irFile: IrFile) {
        val testFunction = mutableListOf<FunctionDescriptor>()
        val beforeFunction = mutableListOf<FunctionDescriptor>()
        val afterFunction = mutableListOf<FunctionDescriptor>()
        val beforeClassFunction = mutableListOf<FunctionDescriptor>()
        val afterClassFunction = mutableListOf<FunctionDescriptor>()
        val testClasses = mutableListOf<ClassDescriptor>()
        irFile.acceptChildrenVoid(object: IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                val classDescriptor = declaration.descriptor
                val doNotprocess = declaration.descriptor.staticScope.contributedMethods.none{it.junitFunction()}
                if (!doNotprocess) {
                    testClasses.add(declaration.descriptor)
                    declaration.acceptChildrenVoid(object:IrElementVisitorVoid{
                        override fun visitElement(element: IrElement) {
                            element.acceptChildrenVoid(this)
                        }

                        override fun visitFunction(declaration: IrFunction) {
                            processFunction(declaration.descriptor)
                        }
                    })
                }
            }

            override fun visitFunction(declaration: IrFunction) {
                processFunction(declaration.descriptor)
            }

            private fun processFunction(descriptor: FunctionDescriptor) {
                when {
                    descriptor.test() -> testFunction.add(descriptor)
                    descriptor.before() -> beforeFunction.add(descriptor)
                    descriptor.after() -> afterFunction.add(descriptor)
                    descriptor.beforeClass() -> beforeClassFunction.add(descriptor)
                    descriptor.afterClass() -> afterClassFunction.add(descriptor)
                }
            }
        })
        /**
         * TODO: for all test classes generate registration...
         */

    }

    private fun FunctionDescriptor.junitFunction() =
            annotations.any { annotation ->
                hasJunitAnnotation(annotation)
            }


    private fun hasJunitAnnotation(annotation: AnnotationDescriptor): Boolean {
        return annotation.fqName == fqNameTestAnnotation &&
                annotation.fqName == fqNameBeforeAnnotation &&
                annotation.fqName == fqNameAfterAnnotation &&
                annotation.fqName == fqNameBeforeClassAnnotation &&
                annotation.fqName == fqNameAfterClassAnnotation
    }

    fun FunctionDescriptor.after() = testAnnotationFqName(fqNameAfterAnnotation)
    fun FunctionDescriptor.before() = testAnnotationFqName(fqNameBeforeAnnotation)
    fun FunctionDescriptor.afterClass() = testAnnotationFqName(fqNameAfterClassAnnotation)
    fun FunctionDescriptor.beforeClass() = testAnnotationFqName(fqNameBeforeClassAnnotation)
    fun FunctionDescriptor.test() = testAnnotationFqName(fqNameTestAnnotation)

    fun FunctionDescriptor.testAnnotationFqName(fqName:FqName) = annotations.any { it.fqName == fqNameTestAnnotation }

}