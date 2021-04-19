public StringMethod() : String {

}

open class StringMy() {
}

public class Test : String<caret> {

}

// EXIST: StringMy
// EXIST: String
// ABSENT: StringMethod
// FIR_COMPARISON