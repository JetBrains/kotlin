package beforeTest //package name should be less than imported one

import test.*

fun main() {
    "O".switchMapOnce {

        "K".switchMapOnce {
            "OK"
        }
    }
}