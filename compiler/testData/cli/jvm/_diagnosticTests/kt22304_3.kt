package ttest //package name should be more than imported one

import test.*

fun foo(): String {
    return "O".switchMapOnce {

        "K".switchMapOnce {
            "OK"
        }
    }
}