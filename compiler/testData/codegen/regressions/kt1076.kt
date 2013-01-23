fun box() : String {
 var cnt = 0
 for (len in 4 downTo 1) {
    cnt++
 }

 for (n in (1..5).reversed())
    cnt++

 return if(cnt == 9) "OK" else cnt.toString()
}
