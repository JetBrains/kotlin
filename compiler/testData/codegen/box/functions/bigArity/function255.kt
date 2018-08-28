// !LANGUAGE: +FunctionTypesWithBigArity
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR

class A(val value: Int) {
    // for (i in 1..254) { print("p${"%03d".format(i)}: A, "); if (i % 10 == 0) println() }; println()

    fun lmao(
        p001: A, p002: A, p003: A, p004: A, p005: A, p006: A, p007: A, p008: A, p009: A, p010: A,
        p011: A, p012: A, p013: A, p014: A, p015: A, p016: A, p017: A, p018: A, p019: A, p020: A,
        p021: A, p022: A, p023: A, p024: A, p025: A, p026: A, p027: A, p028: A, p029: A, p030: A,
        p031: A, p032: A, p033: A, p034: A, p035: A, p036: A, p037: A, p038: A, p039: A, p040: A,
        p041: A, p042: A, p043: A, p044: A, p045: A, p046: A, p047: A, p048: A, p049: A, p050: A,
        p051: A, p052: A, p053: A, p054: A, p055: A, p056: A, p057: A, p058: A, p059: A, p060: A,
        p061: A, p062: A, p063: A, p064: A, p065: A, p066: A, p067: A, p068: A, p069: A, p070: A,
        p071: A, p072: A, p073: A, p074: A, p075: A, p076: A, p077: A, p078: A, p079: A, p080: A,
        p081: A, p082: A, p083: A, p084: A, p085: A, p086: A, p087: A, p088: A, p089: A, p090: A,
        p091: A, p092: A, p093: A, p094: A, p095: A, p096: A, p097: A, p098: A, p099: A, p100: A,
        p101: A, p102: A, p103: A, p104: A, p105: A, p106: A, p107: A, p108: A, p109: A, p110: A,
        p111: A, p112: A, p113: A, p114: A, p115: A, p116: A, p117: A, p118: A, p119: A, p120: A,
        p121: A, p122: A, p123: A, p124: A, p125: A, p126: A, p127: A, p128: A, p129: A, p130: A,
        p131: A, p132: A, p133: A, p134: A, p135: A, p136: A, p137: A, p138: A, p139: A, p140: A,
        p141: A, p142: A, p143: A, p144: A, p145: A, p146: A, p147: A, p148: A, p149: A, p150: A,
        p151: A, p152: A, p153: A, p154: A, p155: A, p156: A, p157: A, p158: A, p159: A, p160: A,
        p161: A, p162: A, p163: A, p164: A, p165: A, p166: A, p167: A, p168: A, p169: A, p170: A,
        p171: A, p172: A, p173: A, p174: A, p175: A, p176: A, p177: A, p178: A, p179: A, p180: A,
        p181: A, p182: A, p183: A, p184: A, p185: A, p186: A, p187: A, p188: A, p189: A, p190: A,
        p191: A, p192: A, p193: A, p194: A, p195: A, p196: A, p197: A, p198: A, p199: A, p200: A,
        p201: A, p202: A, p203: A, p204: A, p205: A, p206: A, p207: A, p208: A, p209: A, p210: A,
        p211: A, p212: A, p213: A, p214: A, p215: A, p216: A, p217: A, p218: A, p219: A, p220: A,
        p221: A, p222: A, p223: A, p224: A, p225: A, p226: A, p227: A, p228: A, p229: A, p230: A,
        p231: A, p232: A, p233: A, p234: A, p235: A, p236: A, p237: A, p238: A, p239: A, p240: A,
        p241: A, p242: A, p243: A, p244: A, p245: A, p246: A, p247: A, p248: A, p249: A, p250: A,
        p251: A, p252: A, p253: A, p254: A
    ) {
        check(this, 0)

        // for (i in 1..254) { print("check(p${"%03d".format(i)}, $i)"); if (i % 6 == 0) println() else print("; ") }; println()

        check(p001, 1); check(p002, 2); check(p003, 3); check(p004, 4); check(p005, 5); check(p006, 6)
        check(p007, 7); check(p008, 8); check(p009, 9); check(p010, 10); check(p011, 11); check(p012, 12)
        check(p013, 13); check(p014, 14); check(p015, 15); check(p016, 16); check(p017, 17); check(p018, 18)
        check(p019, 19); check(p020, 20); check(p021, 21); check(p022, 22); check(p023, 23); check(p024, 24)
        check(p025, 25); check(p026, 26); check(p027, 27); check(p028, 28); check(p029, 29); check(p030, 30)
        check(p031, 31); check(p032, 32); check(p033, 33); check(p034, 34); check(p035, 35); check(p036, 36)
        check(p037, 37); check(p038, 38); check(p039, 39); check(p040, 40); check(p041, 41); check(p042, 42)
        check(p043, 43); check(p044, 44); check(p045, 45); check(p046, 46); check(p047, 47); check(p048, 48)
        check(p049, 49); check(p050, 50); check(p051, 51); check(p052, 52); check(p053, 53); check(p054, 54)
        check(p055, 55); check(p056, 56); check(p057, 57); check(p058, 58); check(p059, 59); check(p060, 60)
        check(p061, 61); check(p062, 62); check(p063, 63); check(p064, 64); check(p065, 65); check(p066, 66)
        check(p067, 67); check(p068, 68); check(p069, 69); check(p070, 70); check(p071, 71); check(p072, 72)
        check(p073, 73); check(p074, 74); check(p075, 75); check(p076, 76); check(p077, 77); check(p078, 78)
        check(p079, 79); check(p080, 80); check(p081, 81); check(p082, 82); check(p083, 83); check(p084, 84)
        check(p085, 85); check(p086, 86); check(p087, 87); check(p088, 88); check(p089, 89); check(p090, 90)
        check(p091, 91); check(p092, 92); check(p093, 93); check(p094, 94); check(p095, 95); check(p096, 96)
        check(p097, 97); check(p098, 98); check(p099, 99); check(p100, 100); check(p101, 101); check(p102, 102)
        check(p103, 103); check(p104, 104); check(p105, 105); check(p106, 106); check(p107, 107); check(p108, 108)
        check(p109, 109); check(p110, 110); check(p111, 111); check(p112, 112); check(p113, 113); check(p114, 114)
        check(p115, 115); check(p116, 116); check(p117, 117); check(p118, 118); check(p119, 119); check(p120, 120)
        check(p121, 121); check(p122, 122); check(p123, 123); check(p124, 124); check(p125, 125); check(p126, 126)
        check(p127, 127); check(p128, 128); check(p129, 129); check(p130, 130); check(p131, 131); check(p132, 132)
        check(p133, 133); check(p134, 134); check(p135, 135); check(p136, 136); check(p137, 137); check(p138, 138)
        check(p139, 139); check(p140, 140); check(p141, 141); check(p142, 142); check(p143, 143); check(p144, 144)
        check(p145, 145); check(p146, 146); check(p147, 147); check(p148, 148); check(p149, 149); check(p150, 150)
        check(p151, 151); check(p152, 152); check(p153, 153); check(p154, 154); check(p155, 155); check(p156, 156)
        check(p157, 157); check(p158, 158); check(p159, 159); check(p160, 160); check(p161, 161); check(p162, 162)
        check(p163, 163); check(p164, 164); check(p165, 165); check(p166, 166); check(p167, 167); check(p168, 168)
        check(p169, 169); check(p170, 170); check(p171, 171); check(p172, 172); check(p173, 173); check(p174, 174)
        check(p175, 175); check(p176, 176); check(p177, 177); check(p178, 178); check(p179, 179); check(p180, 180)
        check(p181, 181); check(p182, 182); check(p183, 183); check(p184, 184); check(p185, 185); check(p186, 186)
        check(p187, 187); check(p188, 188); check(p189, 189); check(p190, 190); check(p191, 191); check(p192, 192)
        check(p193, 193); check(p194, 194); check(p195, 195); check(p196, 196); check(p197, 197); check(p198, 198)
        check(p199, 199); check(p200, 200); check(p201, 201); check(p202, 202); check(p203, 203); check(p204, 204)
        check(p205, 205); check(p206, 206); check(p207, 207); check(p208, 208); check(p209, 209); check(p210, 210)
        check(p211, 211); check(p212, 212); check(p213, 213); check(p214, 214); check(p215, 215); check(p216, 216)
        check(p217, 217); check(p218, 218); check(p219, 219); check(p220, 220); check(p221, 221); check(p222, 222)
        check(p223, 223); check(p224, 224); check(p225, 225); check(p226, 226); check(p227, 227); check(p228, 228)
        check(p229, 229); check(p230, 230); check(p231, 231); check(p232, 232); check(p233, 233); check(p234, 234)
        check(p235, 235); check(p236, 236); check(p237, 237); check(p238, 238); check(p239, 239); check(p240, 240)
        check(p241, 241); check(p242, 242); check(p243, 243); check(p244, 244); check(p245, 245); check(p246, 246)
        check(p247, 247); check(p248, 248); check(p249, 249); check(p250, 250); check(p251, 251); check(p252, 252)
        check(p253, 253); check(p254, 254)
    }

    private fun check(a: A, value: Int) {
        if (a.value != value) {
            throw AssertionError("Expected $value, actual ${a.value}")
        }
    }
}

fun box(): String {
    val ref = A(0)::lmao

    // for (i in 1..254) { print("A($i), "); if (i % 12 == 0) println() }; println()
    ref(
        A(1), A(2), A(3), A(4), A(5), A(6), A(7), A(8), A(9), A(10), A(11), A(12),
        A(13), A(14), A(15), A(16), A(17), A(18), A(19), A(20), A(21), A(22), A(23), A(24),
        A(25), A(26), A(27), A(28), A(29), A(30), A(31), A(32), A(33), A(34), A(35), A(36),
        A(37), A(38), A(39), A(40), A(41), A(42), A(43), A(44), A(45), A(46), A(47), A(48),
        A(49), A(50), A(51), A(52), A(53), A(54), A(55), A(56), A(57), A(58), A(59), A(60),
        A(61), A(62), A(63), A(64), A(65), A(66), A(67), A(68), A(69), A(70), A(71), A(72),
        A(73), A(74), A(75), A(76), A(77), A(78), A(79), A(80), A(81), A(82), A(83), A(84),
        A(85), A(86), A(87), A(88), A(89), A(90), A(91), A(92), A(93), A(94), A(95), A(96),
        A(97), A(98), A(99), A(100), A(101), A(102), A(103), A(104), A(105), A(106), A(107), A(108),
        A(109), A(110), A(111), A(112), A(113), A(114), A(115), A(116), A(117), A(118), A(119), A(120),
        A(121), A(122), A(123), A(124), A(125), A(126), A(127), A(128), A(129), A(130), A(131), A(132),
        A(133), A(134), A(135), A(136), A(137), A(138), A(139), A(140), A(141), A(142), A(143), A(144),
        A(145), A(146), A(147), A(148), A(149), A(150), A(151), A(152), A(153), A(154), A(155), A(156),
        A(157), A(158), A(159), A(160), A(161), A(162), A(163), A(164), A(165), A(166), A(167), A(168),
        A(169), A(170), A(171), A(172), A(173), A(174), A(175), A(176), A(177), A(178), A(179), A(180),
        A(181), A(182), A(183), A(184), A(185), A(186), A(187), A(188), A(189), A(190), A(191), A(192),
        A(193), A(194), A(195), A(196), A(197), A(198), A(199), A(200), A(201), A(202), A(203), A(204),
        A(205), A(206), A(207), A(208), A(209), A(210), A(211), A(212), A(213), A(214), A(215), A(216),
        A(217), A(218), A(219), A(220), A(221), A(222), A(223), A(224), A(225), A(226), A(227), A(228),
        A(229), A(230), A(231), A(232), A(233), A(234), A(235), A(236), A(237), A(238), A(239), A(240),
        A(241), A(242), A(243), A(244), A(245), A(246), A(247), A(248), A(249), A(250), A(251), A(252),
        A(253), A(254)
    )

    return "OK"
}
