// MEMBER_NAME_FILTER: result
// RESOLVE_PROPERTY_PART: GETTER
interface A<T> {
    var result: T
}

class <caret>B(a: A<String>): A<String> by a
