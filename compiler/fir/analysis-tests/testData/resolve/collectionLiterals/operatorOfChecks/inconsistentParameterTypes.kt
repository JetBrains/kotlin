// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS

class MyList1<T> {
    typealias Self<U> = MyList1<U>
    companion object {
        operator fun <K> of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("K (of fun <K> of)")!>single: K?<!>): Self<K> = Self()
        operator fun <L> of(vararg ls: L): Self<L> = Self()
    }
}

class MyList2<T> {
    typealias Self<U> = MyList2<U>
    companion object {
        operator fun <K> of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("K (of fun <K> of)")!>single: K & Any<!>): Self<K> = Self()
        operator fun <L> of(vararg ls: L): Self<L> = Self()
    }
}

class MyList3<T> {
    typealias Self<U> = MyList3<U>
    companion object {
        operator fun <K> of(<!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("Int")!>single: K<!>): Self<K> = Self()
        operator fun <L> of(vararg ls: Int): Self<L> = Self()
    }
}

class MyList4<T> {
    typealias Self<U> = MyList4<U>
    class Box<B>
    companion object {
        operator fun <K> of(
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>a: K<!>,
            b: Box<K>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>c: Box<out K><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>d: Box<in K><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>e: Box<*><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>f: Box<K & Any><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>g: Box<K?><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>h: Box<String><!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>i: Box<K>?<!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("MyList4.Box<K (of fun <K> of)>")!>j: () -> Box<K><!>,
        ): Self<K> = Self()

        operator fun <L> of(vararg ls: Box<L>): Self<L> = Self()
    }
}

class MyList5<T> {
    typealias Self<U> = MyList5<U>
    companion object {
        operator fun of(
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("String")!>a: String?<!>,
            <!INCONSISTENT_PARAMETER_TYPES_IN_OF_OVERLOADS("String")!>b: <!PLATFORM_CLASS_MAPPED_TO_KOTLIN("kotlin.String")!>java.lang.String<!><!>,
            c: String,
            vararg ls: String,
        ): Self<String> = Self()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, dnnType, functionDeclaration, functionalType, inProjection,
nestedClass, nullableType, objectDeclaration, operator, outProjection, starProjection, typeAliasDeclaration,
typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
