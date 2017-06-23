import java.util.Collections.singletonList

fun foo() {
    singl<caret>
}

// INVOCATION_COUNT: 1
// EXIST_JAVA_ONLY: { allLookupStrings: "singleton", itemText: "Collections.singleton", tailText: "(o: T!) (java.util)", typeText: "(Mutable)Set<T!>!", attributes: "" }
// ABSENT: { itemText: "Collections.singletonList" }
// EXIST_JAVA_ONLY: { itemText: "singletonList", tailText: "(o: T!)", typeText: "(Mutable)List<T!>!", attributes: "" }
