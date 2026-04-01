// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTIC_ARGUMENTS
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
//  ^ difference in diagnostic renderer output

class MyList1<T> {
    typealias Self<U> = MyList1<U>
    companion object {
        operator fun <K> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList1<K (of fun <K> of)>")!>Self<String><!> = Self()
        operator fun <L> of(vararg ls: L): Self<L> = Self()
    }
}

class MyList2<T> {
    typealias Self<U> = MyList2<U>
    companion object {
        operator fun <K> <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList2<String>")!>of<!>() = Self<K>()
        operator fun <L> of(vararg ls: L): Self<String> = Self()
    }
}

class MyList3<T> {
    typealias Self<U> = MyList3<U>
    companion object {
        operator fun <K> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList3<K (of fun <K> of)>")!>Self<out K><!> = Self()
        operator fun <L> of(vararg ls: L): Self<L> = Self()
    }
}

class MyList4<T> {
    typealias Self<U> = MyList4<U>
    companion object {
        operator fun <K> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList4<K (of fun <K> of)>")!>Self<K?><!> = Self()
        operator fun <L> of(vararg ls: L): Self<L> = Self()
    }
}

class MyList5<T> {
    typealias Self<U> = MyList5<U>
    companion object {
        operator fun <K> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList5<MyList5<MyList5<K (of fun <K> of)>>>")!>Self<Self<K>><!> = Self()
        operator fun <L> of(vararg ls: L): Self<Self<Self<L>>> = Self()
    }
}

class MyList6<T> {
    typealias Self<U> = MyList6<U>
    companion object {
        operator fun <K> of(): <!INCONSISTENT_RETURN_TYPES_IN_OF_OVERLOADS("MyList6<K (of fun <K> of) & Any>")!>Self<K><!> = Self()
        operator fun <L> of(vararg ls: L): Self<L & Any> = Self()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, nullableType, objectDeclaration, operator,
outProjection, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter, vararg */
