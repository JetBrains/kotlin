// ISSUE: KT-50788
// FILE: Mapper.java
public class Mapper {
    public <T> T readValue(Class<T> valueType) {
        return null;
    }
}

// FILE: main.kt

fun <T : CharSequence?> foo(mapper: Mapper, cls: Class<T>?) {
    val result = <!DEBUG_INFO_EXPRESSION_TYPE("T!!")!>mapper.readValue<T>(cls)!!<!> // The type of result is expected to be T & Any here, but in fact it's just T
    result.length
}
