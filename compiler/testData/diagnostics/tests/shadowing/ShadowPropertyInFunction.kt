class RedefinePropertyInFunction() {

    var i = 17
    
    fun f(): Int {
        var i = 18
        return i
    }

}
