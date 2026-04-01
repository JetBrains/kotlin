suspend fun await(num: Int) {}
fun condition() = true
fun cond() = true

fun someInt() = 1

fun box(): String {
    return "OK"
}

suspend fun test10SP() {
    var num1 = 0
    var num2 = 0
    if (condition()) {
        num1++
        await(0)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(2)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(4)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(6)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(8)
        num2 = num2 + num1
        await(num2)
    }
}

/* Time-consuming tests are commented out. Shall be moved/converted to benchmarks as part of KT-85173.

suspend fun test100SP() {
    var num1 = 0
    var num2 = 0
    if (condition()) {
        num1++
        await(0)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(2)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(4)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(6)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(8)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(10)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(12)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(14)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(16)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(18)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(20)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(22)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(24)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(26)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(28)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(30)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(32)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(34)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(36)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(38)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(40)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(42)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(44)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(46)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(48)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(50)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(52)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(54)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(56)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(58)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(60)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(62)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(64)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(66)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(68)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(70)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(72)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(74)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(76)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(78)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(80)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(82)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(84)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(86)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(88)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(90)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(92)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(94)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(96)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(98)
        num2 = num2 + num1
        await(num2)
    }
}

suspend fun test100SPsComplexCFG() {
    var num = 0
    while (true) {
        when (someInt()) {
            1 -> if (cond()) await(1)
            2 -> if (cond()) await(2)
            3 -> if (cond()) await(3)
            4 -> if (cond()) await(4)
            5 -> if (cond()) await(5)
            6 -> if (cond()) await(6)
            7 -> if (cond()) await(7)
            8 -> if (cond()) await(8)
            9 -> if (cond()) await(9)
            10 -> if (cond()) await(10)
            11 -> if (cond()) await(11)
            12 -> if (cond()) await(12)
            13 -> if (cond()) await(13)
            14 -> if (cond()) await(14)
            15 -> if (cond()) await(15)
            16 -> if (cond()) await(16)
            17 -> if (cond()) await(17)
            18 -> if (cond()) await(18)
            19 -> if (cond()) await(19)
            20 -> if (cond()) await(20)
            21 -> if (cond()) await(21)
            22 -> if (cond()) await(22)
            23 -> if (cond()) await(23)
            24 -> if (cond()) await(24)
            25 -> if (cond()) await(25)
            26 -> if (cond()) await(26)
            27 -> if (cond()) await(27)
            28 -> if (cond()) await(28)
            29 -> if (cond()) await(29)
            30 -> if (cond()) await(30)
            31 -> if (cond()) await(31)
            32 -> if (cond()) await(32)
            33 -> if (cond()) await(33)
            34 -> if (cond()) await(34)
            35 -> if (cond()) await(35)
            36 -> if (cond()) await(36)
            37 -> if (cond()) await(37)
            38 -> if (cond()) await(38)
            39 -> if (cond()) await(39)
            40 -> if (cond()) await(40)
            41 -> if (cond()) await(41)
            42 -> if (cond()) await(42)
            43 -> if (cond()) await(43)
            44 -> if (cond()) await(44)
            45 -> if (cond()) await(45)
            46 -> if (cond()) await(46)
            47 -> if (cond()) await(47)
            48 -> if (cond()) await(48)
            49 -> if (cond()) await(49)
            50 -> if (cond()) await(50)
            51 -> if (cond()) await(51)
            52 -> if (cond()) await(52)
            53 -> if (cond()) await(53)
            54 -> if (cond()) await(54)
            55 -> if (cond()) await(55)
            56 -> if (cond()) await(56)
            57 -> if (cond()) await(57)
            58 -> if (cond()) await(58)
            59 -> if (cond()) await(59)
            60 -> if (cond()) await(60)
            61 -> if (cond()) await(61)
            62 -> if (cond()) await(62)
            63 -> if (cond()) await(63)
            64 -> if (cond()) await(64)
            65 -> if (cond()) await(65)
            66 -> if (cond()) await(66)
            67 -> if (cond()) await(67)
            68 -> if (cond()) await(68)
            69 -> if (cond()) await(69)
            70 -> if (cond()) await(70)
            71 -> if (cond()) await(71)
            72 -> if (cond()) await(72)
            73 -> if (cond()) await(73)
            74 -> if (cond()) await(74)
            75 -> if (cond()) await(75)
            76 -> if (cond()) await(76)
            77 -> if (cond()) await(77)
            78 -> if (cond()) await(78)
            79 -> if (cond()) await(79)
            80 -> if (cond()) await(80)
            81 -> if (cond()) await(81)
            82 -> if (cond()) await(82)
            83 -> if (cond()) await(83)
            84 -> if (cond()) await(84)
            85 -> if (cond()) await(85)
            86 -> if (cond()) await(86)
            87 -> if (cond()) await(87)
            88 -> if (cond()) await(88)
            89 -> if (cond()) await(89)
            90 -> if (cond()) await(90)
            91 -> if (cond()) await(91)
            92 -> if (cond()) await(92)
            93 -> if (cond()) await(93)
            94 -> if (cond()) await(94)
            95 -> if (cond()) await(95)
            96 -> if (cond()) await(96)
            97 -> if (cond()) await(97)
            98 -> if (cond()) await(98)
            99 -> if (cond()) await(99)
            100 -> if (cond()) await(100)
            else -> return
        }
    }
}

suspend fun test1000SP() {
    var num1 = 0
    var num2 = 0
    if (condition()) {
        num1++
        await(0)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(2)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(4)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(6)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(8)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(10)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(12)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(14)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(16)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(18)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(20)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(22)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(24)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(26)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(28)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(30)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(32)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(34)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(36)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(38)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(40)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(42)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(44)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(46)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(48)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(50)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(52)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(54)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(56)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(58)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(60)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(62)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(64)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(66)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(68)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(70)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(72)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(74)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(76)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(78)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(80)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(82)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(84)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(86)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(88)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(90)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(92)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(94)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(96)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(98)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(100)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(102)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(104)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(106)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(108)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(110)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(112)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(114)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(116)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(118)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(120)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(122)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(124)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(126)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(128)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(130)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(132)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(134)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(136)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(138)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(140)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(142)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(144)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(146)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(148)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(150)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(152)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(154)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(156)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(158)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(160)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(162)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(164)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(166)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(168)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(170)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(172)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(174)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(176)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(178)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(180)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(182)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(184)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(186)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(188)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(190)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(192)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(194)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(196)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(198)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(200)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(202)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(204)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(206)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(208)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(210)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(212)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(214)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(216)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(218)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(220)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(222)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(224)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(226)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(228)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(230)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(232)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(234)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(236)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(238)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(240)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(242)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(244)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(246)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(248)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(250)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(252)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(254)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(256)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(258)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(260)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(262)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(264)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(266)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(268)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(270)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(272)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(274)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(276)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(278)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(280)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(282)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(284)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(286)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(288)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(290)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(292)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(294)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(296)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(298)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(300)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(302)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(304)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(306)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(308)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(310)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(312)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(314)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(316)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(318)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(320)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(322)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(324)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(326)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(328)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(330)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(332)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(334)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(336)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(338)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(340)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(342)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(344)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(346)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(348)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(350)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(352)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(354)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(356)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(358)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(360)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(362)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(364)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(366)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(368)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(370)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(372)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(374)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(376)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(378)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(380)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(382)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(384)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(386)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(388)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(390)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(392)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(394)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(396)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(398)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(400)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(402)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(404)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(406)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(408)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(410)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(412)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(414)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(416)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(418)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(420)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(422)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(424)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(426)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(428)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(430)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(432)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(434)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(436)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(438)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(440)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(442)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(444)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(446)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(448)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(450)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(452)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(454)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(456)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(458)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(460)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(462)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(464)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(466)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(468)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(470)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(472)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(474)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(476)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(478)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(480)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(482)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(484)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(486)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(488)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(490)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(492)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(494)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(496)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(498)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(500)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(502)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(504)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(506)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(508)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(510)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(512)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(514)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(516)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(518)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(520)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(522)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(524)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(526)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(528)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(530)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(532)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(534)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(536)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(538)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(540)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(542)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(544)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(546)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(548)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(550)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(552)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(554)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(556)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(558)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(560)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(562)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(564)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(566)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(568)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(570)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(572)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(574)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(576)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(578)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(580)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(582)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(584)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(586)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(588)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(590)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(592)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(594)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(596)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(598)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(600)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(602)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(604)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(606)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(608)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(610)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(612)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(614)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(616)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(618)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(620)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(622)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(624)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(626)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(628)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(630)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(632)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(634)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(636)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(638)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(640)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(642)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(644)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(646)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(648)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(650)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(652)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(654)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(656)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(658)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(660)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(662)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(664)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(666)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(668)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(670)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(672)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(674)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(676)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(678)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(680)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(682)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(684)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(686)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(688)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(690)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(692)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(694)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(696)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(698)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(700)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(702)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(704)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(706)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(708)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(710)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(712)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(714)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(716)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(718)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(720)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(722)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(724)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(726)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(728)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(730)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(732)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(734)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(736)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(738)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(740)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(742)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(744)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(746)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(748)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(750)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(752)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(754)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(756)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(758)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(760)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(762)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(764)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(766)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(768)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(770)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(772)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(774)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(776)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(778)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(780)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(782)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(784)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(786)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(788)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(790)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(792)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(794)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(796)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(798)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(800)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(802)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(804)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(806)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(808)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(810)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(812)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(814)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(816)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(818)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(820)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(822)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(824)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(826)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(828)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(830)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(832)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(834)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(836)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(838)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(840)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(842)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(844)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(846)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(848)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(850)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(852)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(854)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(856)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(858)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(860)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(862)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(864)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(866)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(868)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(870)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(872)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(874)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(876)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(878)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(880)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(882)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(884)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(886)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(888)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(890)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(892)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(894)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(896)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(898)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(900)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(902)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(904)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(906)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(908)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(910)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(912)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(914)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(916)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(918)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(920)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(922)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(924)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(926)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(928)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(930)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(932)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(934)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(936)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(938)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(940)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(942)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(944)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(946)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(948)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(950)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(952)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(954)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(956)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(958)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(960)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(962)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(964)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(966)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(968)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(970)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(972)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(974)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(976)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(978)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(980)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(982)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(984)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(986)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(988)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(990)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(992)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(994)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(996)
        num2 = num2 + num1
        await(num2)
    }
    if (condition()) {
        num1++
        await(998)
        num2 = num2 + num1
        await(num2)
    }
}

suspend fun test50SPs50Vars() {
    var unused_1 = 1
    var unused_2 = 2
    var unused_3 = 3
    var unused_4 = 4
    var unused_5 = 5
    var unused_6 = 6
    var unused_7 = 7
    var unused_8 = 8
    var unused_9 = 9
    var unused_10 = 10
    var unused_11 = 11
    var unused_12 = 12
    var unused_13 = 13
    var unused_14 = 14
    var unused_15 = 15
    var unused_16 = 16
    var unused_17 = 17
    var unused_18 = 18
    var unused_19 = 19
    var unused_20 = 20
    var unused_21 = 21
    var unused_22 = 22
    var unused_23 = 23
    var unused_24 = 24
    var unused_25 = 25
    var unused_26 = 26
    var unused_27 = 27
    var unused_28 = 28
    var unused_29 = 29
    var unused_30 = 30
    var unused_31 = 31
    var unused_32 = 32
    var unused_33 = 33
    var unused_34 = 34
    var unused_35 = 35
    var unused_36 = 36
    var unused_37 = 37
    var unused_38 = 38
    var unused_39 = 39
    var unused_40 = 40
    var unused_41 = 41
    var unused_42 = 42
    var unused_43 = 43
    var unused_44 = 44
    var unused_45 = 45
    var unused_46 = 46
    var unused_47 = 47
    var unused_48 = 48
    var unused_49 = 49
    var unused_50 = 50

    await(1)
    await(2)
    await(3)
    await(4)
    await(5)
    await(6)
    await(7)
    await(8)
    await(9)
    await(10)
    await(11)
    await(12)
    await(13)
    await(14)
    await(15)
    await(16)
    await(17)
    await(18)
    await(19)
    await(20)
    await(21)
    await(22)
    await(23)
    await(24)
    await(25)
    await(26)
    await(27)
    await(28)
    await(29)
    await(30)
    await(31)
    await(32)
    await(33)
    await(34)
    await(35)
    await(36)
    await(37)
    await(38)
    await(39)
    await(40)
    await(41)
    await(42)
    await(43)
    await(44)
    await(45)
    await(46)
    await(47)
    await(48)
    await(49)
    await(50)
}

suspend fun test50SPs50VarsComplexCFG() {
    var unused_1 = 1
    var unused_2 = 2
    var unused_3 = 3
    var unused_4 = 4
    var unused_5 = 5
    var unused_6 = 6
    var unused_7 = 7
    var unused_8 = 8
    var unused_9 = 9
    var unused_10 = 10
    var unused_11 = 11
    var unused_12 = 12
    var unused_13 = 13
    var unused_14 = 14
    var unused_15 = 15
    var unused_16 = 16
    var unused_17 = 17
    var unused_18 = 18
    var unused_19 = 19
    var unused_20 = 20
    var unused_21 = 21
    var unused_22 = 22
    var unused_23 = 23
    var unused_24 = 24
    var unused_25 = 25
    var unused_26 = 26
    var unused_27 = 27
    var unused_28 = 28
    var unused_29 = 29
    var unused_30 = 30
    var unused_31 = 31
    var unused_32 = 32
    var unused_33 = 33
    var unused_34 = 34
    var unused_35 = 35
    var unused_36 = 36
    var unused_37 = 37
    var unused_38 = 38
    var unused_39 = 39
    var unused_40 = 40
    var unused_41 = 41
    var unused_42 = 42
    var unused_43 = 43
    var unused_44 = 44
    var unused_45 = 45
    var unused_46 = 46
    var unused_47 = 47
    var unused_48 = 48
    var unused_49 = 49
    var unused_50 = 50

    while (true) {
        when (someInt()) {
            1 -> if (cond()) await(1)
            2 -> if (cond()) await(2)
            3 -> if (cond()) await(3)
            4 -> if (cond()) await(4)
            5 -> if (cond()) await(5)
            6 -> if (cond()) await(6)
            7 -> if (cond()) await(7)
            8 -> if (cond()) await(8)
            9 -> if (cond()) await(9)
            10 -> if (cond()) await(10)
            11 -> if (cond()) await(11)
            12 -> if (cond()) await(12)
            13 -> if (cond()) await(13)
            14 -> if (cond()) await(14)
            15 -> if (cond()) await(15)
            16 -> if (cond()) await(16)
            17 -> if (cond()) await(17)
            18 -> if (cond()) await(18)
            19 -> if (cond()) await(19)
            20 -> if (cond()) await(20)
            21 -> if (cond()) await(21)
            22 -> if (cond()) await(22)
            23 -> if (cond()) await(23)
            24 -> if (cond()) await(24)
            25 -> if (cond()) await(25)
            26 -> if (cond()) await(26)
            27 -> if (cond()) await(27)
            28 -> if (cond()) await(28)
            29 -> if (cond()) await(29)
            30 -> if (cond()) await(30)
            31 -> if (cond()) await(31)
            32 -> if (cond()) await(32)
            33 -> if (cond()) await(33)
            34 -> if (cond()) await(34)
            35 -> if (cond()) await(35)
            36 -> if (cond()) await(36)
            37 -> if (cond()) await(37)
            38 -> if (cond()) await(38)
            39 -> if (cond()) await(39)
            40 -> if (cond()) await(40)
            41 -> if (cond()) await(41)
            42 -> if (cond()) await(42)
            43 -> if (cond()) await(43)
            44 -> if (cond()) await(44)
            45 -> if (cond()) await(45)
            46 -> if (cond()) await(46)
            47 -> if (cond()) await(47)
            48 -> if (cond()) await(48)
            49 -> if (cond()) await(49)
            50 -> if (cond()) await(50)
            else -> return
        }
    }
}
*/