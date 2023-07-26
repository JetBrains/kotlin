@Target(AnnotationTarget.TYPE)
annotation class Anno/* NonReanalyzableClassDeclarationStructureElement */

open class A/* NonReanalyzableClassDeclarationStructureElement */

class B : @Anno A()/* NonReanalyzableClassDeclarationStructureElement */
