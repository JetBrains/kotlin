// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT

class DirectMismatch {
    companion object {
        operator fun <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>of<!>(): DirectMismatch = DirectMismatch()
        <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>internal<!> operator fun of(s1: String): DirectMismatch = DirectMismatch()
        <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>protected<!> operator fun of(s1: String, s2: String): DirectMismatch = DirectMismatch()
        <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>public<!> operator fun of(s1: String, s2: String, s3: String): DirectMismatch = DirectMismatch()
        private operator fun of(vararg ss: String): DirectMismatch = DirectMismatch()
    }
}

abstract class OfProvider<T> {
    abstract protected fun of(): T
    abstract protected fun of(s1: String): T
}

class InheritedMismatch {
    companion object : OfProvider<InheritedMismatch>() {
        override operator fun <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>of<!>(): InheritedMismatch = InheritedMismatch()
        public override operator fun of(s1: String): InheritedMismatch = InheritedMismatch()
        <!INCONSISTENT_VISIBILITY_IN_OF_OVERLOADS!>private<!> operator fun of(s1: String, s2: String): InheritedMismatch = InheritedMismatch()
        operator fun of(vararg ss: String): InheritedMismatch = InheritedMismatch()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, objectDeclaration, operator, vararg */
