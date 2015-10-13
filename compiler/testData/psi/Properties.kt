val foo = bar.foo.bar

val foo
val @[a] foo
val foo.bar

val foo : T
val @[a] foo = bar
val foo.bar
   get() {}
   set(sad) = foo


val foo get
val foo set

var foo
  get
  private set

val foo.bar
   get() {}
   set

val foo.bar
   get
   set(sad) = foo

val foo = 5; get
val foo =1; get set

var foo = 5
  get
  private set

val foo.bar = 5
   get() {}
   set

val foo.bar = 5
   get
   set(sad) = foo

fun foo() {
  val foo = 5
  get() = 5
}

val IList<T>.lastIndex : Int
  get() = this.size - 1

val Int?.opt : Int
val Int? .opt : Int