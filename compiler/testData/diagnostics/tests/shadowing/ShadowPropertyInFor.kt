// FIR_IDENTICAL
class RedefinePropertyInFor() {

    var i = 1
    
    fun ff() {
        for (i in 0..10) {
        }
    }

}

