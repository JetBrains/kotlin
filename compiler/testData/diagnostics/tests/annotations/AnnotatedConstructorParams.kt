package a

import java.lang.Deprecated as deprecated
import java.lang.SuppressWarnings as suppresswarnings


<!DEPRECATED_JAVA_ANNOTATION!>@deprecated<!> @suppresswarnings val s: String = "";

<!DEPRECATED_JAVA_ANNOTATION!>@deprecated<!> @suppresswarnings fun main() {
    System.out.println("Hello, world!")
}

class Test(<!DEPRECATED_JAVA_ANNOTATION!>@deprecated<!> val s: String,
           @suppresswarnings val x : Int) {}

