// "Add label to loop" "true"
fun breakContinueInWhen(i: Int) {
    loop@ for (y in 0..10) {
        when(i) {
            0 -> break@loop
            2 -> {
                for(z in 0..10) {
                    break
                }
                for(w in 0..10) {
                    continue
                }
            }
        }
    }
}
