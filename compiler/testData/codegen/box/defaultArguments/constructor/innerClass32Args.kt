// IGNORE_BACKEND_FIR: JVM_IR
class A {
    inner class B(val a: Int = 1,
            val b: Int = 2,
            val c: Int = 3,
            val d: Int = 4,
            val e: Int = 5,
            val f: Int = 6,
            val g: Int = 7,
            val h: Int = 8,
            val i: Int = 9,
            val j: Int = 10,
            val k: Int = 11,
            val l: Int = 12,
            val m: Int = 13,
            val n: Int = 14,
            val o: Int = 15,
            val p: Int = 16,
            val q: Int = 17,
            val r: Int = 18,
            val s: Int = 19,
            val t: Int = 20,
            val u: Int = 21,
            val v: Int = 22,
            val w: Int = 23,
            val x: Int = 24,
            val y: Int = 25,
            val z: Int = 26,
            val aa: Int = 27,
            val bb: Int = 28,
            val cc: Int = 29,
            val dd: Int = 30,
            val ee: Int = 31,
            val ff: Int = 32) {
        override fun toString(): String {
            return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff"
        }
    }
}

fun box(): String {
    val test1 = A().B().toString()
    if (test1 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32") {
        return "test1 = $test1"
    }

    val test2 = A().B(4, e = 8, f = 15, w = 16, aa = 23, ff = 42).toString()
    if (test2 != "4 2 3 4 8 15 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 16 24 25 26 23 28 29 30 31 42") {
        return "test2 = $test2"
    }

    return "OK"
}
