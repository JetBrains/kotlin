package nestedObjects
  object A {
    val b = B
    val d = A.B.A

    object B {
      val a = A
      val e = B.A

      object A {
        val a = A
        val b = B
        val x = nestedObjects.A.B.A
        val y = this@A
      }
    }

  }
  object B {
    val b = B
    val c = A.B
  }

  val a = A
  val b = B
  val c = A.B
  val d = A.B.A
  val e = B.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: A">A</error>.<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: B">B</error>
