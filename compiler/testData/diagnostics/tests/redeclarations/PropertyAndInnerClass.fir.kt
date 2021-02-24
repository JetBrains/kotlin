public class A() {
    <!REDECLARATION!>public val FOO: String = "test"<!>

    <!REDECLARATION!>public class FOO() { }<!>
}

public class B() {
   companion object {
      <!REDECLARATION!>public val FOO: String = "test"<!>
      
      <!REDECLARATION!>public class FOO() { }<!>
   }
}
