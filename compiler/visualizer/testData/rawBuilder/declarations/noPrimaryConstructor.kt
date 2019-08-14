class NoPrimary {
//      String
//      │
    val x: String

    constructor(x: String) {
//           val (NoPrimary).x: String
//           │   NoPrimary.<init>.x: String
//           │   │
        this.x = x
    }

    constructor(): this("")
}
