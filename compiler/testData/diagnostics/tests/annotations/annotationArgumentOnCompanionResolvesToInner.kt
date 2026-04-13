// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-77137

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class Anno(val value: Int)

abstract class Super(c: Int)

class TopLevelClass() {
    @Anno(<!UNRESOLVED_REFERENCE!>inner<!>)
    companion object : @Anno(<!UNRESOLVED_REFERENCE!>inner<!>) Super(<!INSTANCE_ACCESS_BEFORE_SUPER_CALL!>inner<!>) {
        const val inner = 1
    }
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, companionObject, const, integerLiteral,
objectDeclaration, primaryConstructor, propertyDeclaration */
