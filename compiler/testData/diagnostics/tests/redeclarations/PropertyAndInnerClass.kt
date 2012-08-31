public class A() {
    public val <!REDECLARATION!>FOO<!>: String = "test"

    public class <!REDECLARATION!>FOO<!>() { }
}

public class B() {
   class object {
      public val <!REDECLARATION!>FOO<!>: String = "test"
      
      public class <!REDECLARATION!>FOO<!>() { }
   }
}
