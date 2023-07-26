@Target(AnnotationTarget.TYPE)
annotation class Anno/* NonReanalyzableClassDeclarationStructureElement */

interface A/* NonReanalyzableClassDeclarationStructureElement */

class B : @Anno A/* NonReanalyzableClassDeclarationStructureElement */
