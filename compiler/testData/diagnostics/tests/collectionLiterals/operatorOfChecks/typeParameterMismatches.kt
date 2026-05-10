// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80492
// LANGUAGE: +CollectionLiterals
// RENDER_DIAGNOSTICS_FULL_TEXT
// SUPPRESS_NO_TYPE_ALIAS_EXPANSION_MODE
//  ^ difference in diagnostic renderer output

class MyList1<out T> {
    typealias Self<U> = MyList1<U>
    companion object {
        operator fun <T> of(vararg ts: T): Self<T> = Self()
        <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!>operator fun of(): Self<Nothing> = Self()<!>
    }
}

class MyList2<T> {
    typealias Self<U> = MyList2<U>
    companion object {
        operator fun <T> of(vararg ts: T): Self<T> = Self()
        operator fun <K> of(): Self<K> = Self()
    }
}

class MyList3<T> {
    typealias Self<U> = MyList3<U>
    companion object {
        operator fun <T: Any> of(vararg ts: T): Self<T> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><K><!> of(): Self<K> = Self()
    }
}

class MyList4<T> {
    typealias Self<U> = MyList4<U>
    companion object {
        operator fun <T> of(vararg ts: T): Self<T> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><K: Any><!> of(): Self<K> = Self()
    }
}

class MyList5<T> {
    typealias Self<U> = MyList5<U>
    companion object {
        operator fun <T> of(vararg ts: T): Self<T> = Self()
        operator fun <K> of(): Self<K> = Self()
    }
}

class MyList6<T> {
    typealias Self<U> = MyList6<U>
    companion object {
        operator fun <T: Any> of(vararg ts: T): Self<T> = Self()
        operator fun <K: Any> of(): Self<K> = Self()
    }
}

class MyList7<T> {
    typealias Self<U> = MyList7<U>
    companion object {
        operator fun of(vararg ts: Any): Self<Any> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><K: Any><!> of(): Self<K> = Self()
    }
}

interface Comparable<L>

class MyList8<T> {
    typealias Self<U> = MyList8<U>
    companion object {
        operator fun <K: Comparable<K>> of(vararg ts: K): Self<K> = Self()
        operator fun <T: Comparable<T>> of(): Self<T> = Self()
    }
}

class MyList9<T> {
    typealias Self<U> = MyList9<U>
    companion object {
        operator fun <K: Comparable<K>> of(vararg ts: K): Self<K> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><T: Comparable<Int>><!> of(): Self<T> = Self()
    }
}

class MyList10<T> {
    typealias Self<U> = MyList10<U>
    companion object {
        operator fun <K> of(vararg ts: K): Self<K> = Self()
        operator fun <!INCONSISTENT_TYPE_PARAMETERS_IN_OF_OVERLOADS!><A, B><!> of(): Self<A> = Self()
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, functionDeclaration, interfaceDeclaration, nullableType,
objectDeclaration, operator, out, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeConstraint,
typeParameter, vararg */
