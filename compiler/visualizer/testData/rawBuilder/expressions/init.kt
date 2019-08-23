class WithInit(x: Int) {
//      Int
//      │
    val x: Int

    init {
//           val (WithInit).x: Int
//           │   WithInit.<init>.x: Int
//           │   │
        this.x = x
    }
}
