[inline] fun with<T>(receiver : T, body : {T.() : Unit}) = receiver.body()

fun example() {

  with(java.lang.System.out) {
    println("foo");
    print("bar");
  }

  System.out.{
    println("foo");
    print("bar");
  }()

}