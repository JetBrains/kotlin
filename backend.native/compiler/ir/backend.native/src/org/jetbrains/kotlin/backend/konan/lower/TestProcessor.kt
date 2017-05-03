package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.IrElementVisitorVoidWithContext
import org.jetbrains.kotlin.backend.konan.KonanBackendContext
import org.jetbrains.kotlin.backend.konan.descriptors.contributedMethods
import org.jetbrains.kotlin.backend.konan.llvm.symbolName
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

internal class TestProcessor (val context: KonanBackendContext): FileLoweringPass {

    // TODO: Replace with symbols
    companion object {
        val fqNameTestAnnotation = FqName("kotlin.test.Test")
        val fqNameBeforeAnnotation = FqName("kotlin.test.Before")
        val fqNameAfterAnnotation = FqName("kotlin.test.After")
        val fqNameBeforeClassAnnotation = FqName("kotlin.test.BeforeClass")
        val fqNameAfterClassAnnotation = FqName("kotlin.test.AfterClass")
        val fqNameIgnoreAnnotation = FqName("kotlin.test.Ignore")
    }

    private inner class TestClass(val clazz: IrClassSymbol) {
        val testFunctions = mutableListOf<IrFunctionSymbol>()
        val beforeFunctions = mutableListOf<IrFunctionSymbol>()
        val afterFunctions = mutableListOf<IrFunctionSymbol>()
    }

    private inner class TopLevelTestSuite() {
        val testFunctions = mutableListOf<IrFunctionSymbol>()
        val beforeFunctions = mutableListOf<IrFunctionSymbol>()
        val afterFunctions = mutableListOf<IrFunctionSymbol>()
        val beforeClassFunctions = mutableListOf<IrFunctionSymbol>()
        val afterClassFunctions = mutableListOf<IrFunctionSymbol>()
    }

    private inner class AnnotationCollector : IrElementVisitorVoid {
        val testClasses = mutableMapOf<IrClassSymbol, TestClass>()
        val topLevelTestSuite = TopLevelTestSuite()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }



//        override fun visitClass(declaration: IrClass) {
//            val doNotprocess = declaration.descriptor.staticScope.contributedMethods.none { it.testFunction() }
//            if (!doNotprocess) {
//                testClasses.add(declaration.symbol)
//
//
//
//                declaration.acceptChildrenVoid(object:IrElementVisitorVoid{
//                    override fun visitElement(element: IrElement) {
//                        element.acceptChildrenVoid(this)
//                    }
//
//                    override fun visitFunction(declaration: IrFunction) {
//                        processFunction(declaration.descriptor)
//                    }
//                })
//            }
//        }

        override fun visitFunction(declaration: IrFunction) {
            val descriptor = declaration.descriptor
            val symbol = declaration.symbol

            // TODO: Make it smarter
            //if (descriptor.test())        testFunctions.add(symbol)
            //if (descriptor.before())      beforeFunctions.add(symbol)
            //if (descriptor.after())       afterFunctions.add(symbol)
            //if (descriptor.beforeClass()) beforeClassFunctions.add(symbol)
            //if (descriptor.afterClass())  afterClassFunctions.add(symbol)


        }

    }


    override fun lower(irFile: IrFile) {
        irFile.acceptChildrenVoid(AnnotationCollector())
        /**
         * TODO: for all test classes generate registration...
         */
    }

    private fun FunctionDescriptor.isTestFunction() =
            annotations.any { annotation ->
                hasTestAnnotation(annotation)
            }


    private fun hasTestAnnotation(annotation: AnnotationDescriptor): Boolean {
        return annotation.fqName == fqNameTestAnnotation ||
                annotation.fqName == fqNameBeforeAnnotation ||
                annotation.fqName == fqNameAfterAnnotation ||
                annotation.fqName == fqNameBeforeClassAnnotation ||
                annotation.fqName == fqNameAfterClassAnnotation
    }

    fun FunctionDescriptor.isAfter() = testAnnotationFqName(fqNameAfterAnnotation)
    fun FunctionDescriptor.isBefore() = testAnnotationFqName(fqNameBeforeAnnotation)
    fun FunctionDescriptor.isAfterClass() = testAnnotationFqName(fqNameAfterClassAnnotation)
    fun FunctionDescriptor.isBeforeClass() = testAnnotationFqName(fqNameBeforeClassAnnotation)
    fun FunctionDescriptor.isTest() = testAnnotationFqName(fqNameTestAnnotation)

    fun FunctionDescriptor.testAnnotationFqName(fqName:FqName) = annotations.any { it.fqName == fqNameTestAnnotation }

}