// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME

class A(val a: Int = 1,
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

class B(val a: Int = 1,
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
        val ff: Int = 32,
        val gg: Int = 33,
        val hh: Int = 34,
        val ii: Int = 35,
        val jj: Int = 36,
        val kk: Int = 37,
        val ll: Int = 38,
        val mm: Int = 39,
        val nn: Int = 40) {
    override fun toString(): String {
        return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff " +
                "$gg $hh $ii $jj $kk $ll $mm $nn"
    }
}

class C(val a: Int = 1,
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
        val ff: Int = 32,
        val gg: Int = 33,
        val hh: Int = 34,
        val ii: Int = 35,
        val jj: Int = 36,
        val kk: Int = 37,
        val ll: Int = 38,
        val mm: Int = 39,
        val nn: Int = 40,
        val oo: Int = 41,
        val pp: Int = 42,
        val qq: Int = 43,
        val rr: Int = 44,
        val ss: Int = 45,
        val tt: Int = 46,
        val uu: Int = 47,
        val vv: Int = 48,
        val ww: Int = 49,
        val xx: Int = 50,
        val yy: Int = 51,
        val zz: Int = 52,
        val aaa: Int = 53,
        val bbb: Int = 54,
        val ccc: Int = 55,
        val ddd: Int = 56,
        val eee: Int = 57,
        val fff: Int = 58,
        val ggg: Int = 59,
        val hhh: Int = 60,
        val iii: Int = 61,
        val jjj: Int = 62,
        val kkk: Int = 63,
        val lll: Int = 64,
        val mmm: Int = 65,
        val nnn: Int = 66,
        val ooo: Int = 67,
        val ppp: Int = 68,
        val qqq: Int = 69,
        val rrr: Int = 70) {
    override fun toString(): String {
        return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff $gg $hh $ii $jj $kk " +
                "$ll $mm $nn $oo $pp $qq $rr $ss $tt $uu $vv $ww $xx $yy $zz $aaa $bbb $ccc $ddd $eee $fff $ggg $hhh $iii $jjj $kkk $lll " +
                "$mmm $nnn $ooo $ppp $qqq $rrr"
    }
}

fun box(): String {
    val test1 = A(4, e = 8, f = 15, w = 16, aa = 23, ff = 42).toString()
    val test2 = A::class.java.newInstance().toString()
    val test3 = A(32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, q = 16, r = 15, s = 14, t = 13,
                  u = 12, v = 11, w = 10, x = 9, y = 8, z = 7, aa = 6, bb = 5, cc = 4, dd = 3, ee = 2, ff = 1).toString()
    if (test1 != "4 2 3 4 8 15 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 16 24 25 26 23 28 29 30 31 42") {
        return "test1 = $test1"
    }
    if (test2 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32") {
        return "test2 = $test2"
    }
    if (test3 != "32 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1") {
        return "test3 = $test3"
    }

    val test4 = B(54, 217, h = 236, l = 18, q = 3216, u = 8, aa = 22, ff = 33, jj = 44, mm = 55).toString()
    val test5 = B::class.java.newInstance().toString()
    val test6 = B(40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, u = 20, v = 19,
                  w = 18, x = 17, y = 16, z = 15, aa = 14, bb = 13, cc = 12, dd = 11, ee = 10, ff = 9, gg = 8, hh = 7, ii = 6,
                  jj = 5, kk = 4, ll = 3, mm = 2, nn = 1).toString()
    if (test4 != "54 217 3 4 5 6 7 236 9 10 11 18 13 14 15 16 3216 18 19 20 8 22 23 24 25 26 22 28 29 30 31 33 33 34 35 44 37 38 55 40") {
        return "test4 = $test4"
    }
    if (test5 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40") {
        return "test5 = $test5"
    }
    if (test6 != "40 39 38 37 36 35 34 33 32 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1") {
        return "test6 = $test6"
    }

    val test7 = C(5, f = 3, w = 1, aa = 71, nn = 2, qq = 15, ww = 97, aaa = 261258, iii = 3, nnn = 8, rrr = 7).toString()
    val test8 = C::class.java.newInstance().toString()
    val test9 = C(70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41,
                  40, 39, 38, 37, 36, jj = 35, kk = 34, ll = 33, mm = 32, nn = 31, oo = 30, pp = 29, qq = 28, rr = 27, ss = 26, tt = 25,
                  uu = 24, vv = 23, ww = 22, xx = 21, yy = 20, zz = 19, aaa = 18, bbb = 17, ccc = 16, ddd = 15, eee = 14, fff = 13,
                  ggg = 12, hhh = 11, iii = 10, jjj = 9, kkk = 8, lll = 7, mmm = 6, nnn = 5, ooo = 4, ppp = 3, qqq = 2, rrr = 1).toString()
    if (test7 != "5 2 3 4 5 3 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 1 24 25 26 71 28 29 30 31 32 33 34 35 36 37 38 39 2 41 42 15 " +
            "44 45 46 47 48 97 50 51 52 261258 54 55 56 57 58 59 60 3 62 63 64 65 8 67 68 69 7") {
        return "test7 = $test7"
    }
    if (test8 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40 41 42 " +
            "43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59 60 61 62 63 64 65 66 67 68 69 70") {
        return "test8 = $test8"
    }
    if (test9 != "70 69 68 67 66 65 64 63 62 61 60 59 58 57 56 55 54 53 52 51 50 49 48 47 46 45 44 43 42 41 40 39 38 37 36 35 34 33 32 " +
            "31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1") {
        return "test9 = $test9"
    }

    return "OK"
}
