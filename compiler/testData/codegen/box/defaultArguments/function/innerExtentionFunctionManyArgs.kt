// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun Int.foo(a: Int = 1,
                b: Int = 2,
                c: Int = 3,
                d: Int = 4,
                e: Int = 5,
                f: Int = 6,
                g: Int = 7,
                h: Int = 8,
                i: Int = 9,
                j: Int = 10,
                k: Int = 11,
                l: Int = 12,
                m: Int = 13,
                n: Int = 14,
                o: Int = 15,
                p: Int = 16,
                q: Int = 17,
                r: Int = 18,
                s: Int = 19,
                t: Int = 20,
                u: Int = 21,
                v: Int = 22,
                w: Int = 23,
                x: Int = 24,
                y: Int = 25,
                z: Int = 26,
                aa: Int = 27,
                bb: Int = 28,
                cc: Int = 29,
                dd: Int = 30,
                ee: Int = 31,
                ff: Int = 32): String {
        return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff"
    }

    fun String.bar(a: Int = 1,
                   b: Int = 2,
                   c: Int = 3,
                   d: Int = 4,
                   e: Int = 5,
                   f: Int = 6,
                   g: Int = 7,
                   h: Int = 8,
                   i: Int = 9,
                   j: Int = 10,
                   k: Int = 11,
                   l: Int = 12,
                   m: Int = 13,
                   n: Int = 14,
                   o: Int = 15,
                   p: Int = 16,
                   q: Int = 17,
                   r: Int = 18,
                   s: Int = 19,
                   t: Int = 20,
                   u: Int = 21,
                   v: Int = 22,
                   w: Int = 23,
                   x: Int = 24,
                   y: Int = 25,
                   z: Int = 26,
                   aa: Int = 27,
                   bb: Int = 28,
                   cc: Int = 29,
                   dd: Int = 30,
                   ee: Int = 31,
                   ff: Int = 32,
                   gg: Int = 33,
                   hh: Int = 34,
                   ii: Int = 35,
                   jj: Int = 36,
                   kk: Int = 37,
                   ll: Int = 38,
                   mm: Int = 39,
                   nn: Int = 40): String {
        return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff " +
                "$gg $hh $ii $jj $kk $ll $mm $nn"
    }

    fun Char.baz(a: Int = 1,
                 b: Int = 2,
                 c: Int = 3,
                 d: Int = 4,
                 e: Int = 5,
                 f: Int = 6,
                 g: Int = 7,
                 h: Int = 8,
                 i: Int = 9,
                 j: Int = 10,
                 k: Int = 11,
                 l: Int = 12,
                 m: Int = 13,
                 n: Int = 14,
                 o: Int = 15,
                 p: Int = 16,
                 q: Int = 17,
                 r: Int = 18,
                 s: Int = 19,
                 t: Int = 20,
                 u: Int = 21,
                 v: Int = 22,
                 w: Int = 23,
                 x: Int = 24,
                 y: Int = 25,
                 z: Int = 26,
                 aa: Int = 27,
                 bb: Int = 28,
                 cc: Int = 29,
                 dd: Int = 30,
                 ee: Int = 31,
                 ff: Int = 32,
                 gg: Int = 33,
                 hh: Int = 34,
                 ii: Int = 35,
                 jj: Int = 36,
                 kk: Int = 37,
                 ll: Int = 38,
                 mm: Int = 39,
                 nn: Int = 40,
                 oo: Int = 41,
                 pp: Int = 42,
                 qq: Int = 43,
                 rr: Int = 44,
                 ss: Int = 45,
                 tt: Int = 46,
                 uu: Int = 47,
                 vv: Int = 48,
                 ww: Int = 49,
                 xx: Int = 50,
                 yy: Int = 51,
                 zz: Int = 52,
                 aaa: Int = 53,
                 bbb: Int = 54,
                 ccc: Int = 55,
                 ddd: Int = 56,
                 eee: Int = 57,
                 fff: Int = 58,
                 ggg: Int = 59,
                 hhh: Int = 60,
                 iii: Int = 61,
                 jjj: Int = 62,
                 kkk: Int = 63,
                 lll: Int = 64,
                 mmm: Int = 65,
                 nnn: Int = 66,
                 ooo: Int = 67,
                 ppp: Int = 68,
                 qqq: Int = 69,
                 rrr: Int = 70): String {
        return "$a $b $c $d $e $f $g $h $i $j $k $l $m $n $o $p $q $r $s $t $u $v $w $x $y $z $aa $bb $cc $dd $ee $ff $gg $hh $ii $jj $kk " +
                "$ll $mm $nn $oo $pp $qq $rr $ss $tt $uu $vv $ww $xx $yy $zz $aaa $bbb $ccc $ddd $eee $fff $ggg $hhh $iii $jjj $kkk $lll " +
                "$mmm $nnn $ooo $ppp $qqq $rrr"
    }

    fun test(): String {
        val test1 = 1.foo(4, e = 8, f = 15, w = 16, aa = 23, ff = 42)
        val test2 = 1.foo()
        val test3 = 1.foo(32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, q = 16, r = 15, s = 14, t = 13,
                          u = 12, v = 11, w = 10, x = 9, y = 8, z = 7, aa = 6, bb = 5, cc = 4, dd = 3, ee = 2, ff = 1)
        if (test1 != "4 2 3 4 8 15 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 16 24 25 26 23 28 29 30 31 42") {
            return "test1 = $test1"
        }
        if (test2 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32") {
            return "test2 = $test2"
        }
        if (test3 != "32 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1") {
            return "test3 = $test3"
        }

        val test4 = "".bar(54, 217, h = 236, l = 18, q = 3216, u = 8, aa = 22, ff = 33, jj = 44, mm = 55)
        val test5 = "".bar()
        val test6 = "".bar(40, 39, 38, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, u = 20, v = 19,
                           w = 18, x = 17, y = 16, z = 15, aa = 14, bb = 13, cc = 12, dd = 11, ee = 10, ff = 9, gg = 8, hh = 7, ii = 6,
                           jj = 5, kk = 4, ll = 3, mm = 2, nn = 1)
        if (test4 != "54 217 3 4 5 6 7 236 9 10 11 18 13 14 15 16 3216 18 19 20 8 22 23 24 25 26 22 28 29 30 31 33 33 34 35 44 37 38 55 40") {
            return "test4 = $test4"
        }
        if (test5 != "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40") {
            return "test5 = $test5"
        }
        if (test6 != "40 39 38 37 36 35 34 33 32 31 30 29 28 27 26 25 24 23 22 21 20 19 18 17 16 15 14 13 12 11 10 9 8 7 6 5 4 3 2 1") {
            return "test6 = $test6"
        }

        val test7 = 'a'.baz(5, f = 3, w = 1, aa = 71, nn = 2, qq = 15, ww = 97, aaa = 261258, iii = 3, nnn = 8, rrr = 7)
        val test8 = 'a'.baz()
        val test9 = 'a'.baz(70, 69, 68, 67, 66, 65, 64, 63, 62, 61, 60, 59, 58, 57, 56, 55, 54, 53, 52, 51, 50, 49, 48, 47, 46, 45, 44, 43, 42, 41,
                            40, 39, 38, 37, 36, jj = 35, kk = 34, ll = 33, mm = 32, nn = 31, oo = 30, pp = 29, qq = 28, rr = 27, ss = 26, tt = 25,
                            uu = 24, vv = 23, ww = 22, xx = 21, yy = 20, zz = 19, aaa = 18, bbb = 17, ccc = 16, ddd = 15, eee = 14, fff = 13,
                            ggg = 12, hhh = 11, iii = 10, jjj = 9, kkk = 8, lll = 7, mmm = 6, nnn = 5, ooo = 4, ppp = 3, qqq = 2, rrr = 1)
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
}

fun box(): String  {
    return A().test()
}