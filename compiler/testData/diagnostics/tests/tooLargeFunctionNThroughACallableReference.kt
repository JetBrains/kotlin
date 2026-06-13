// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-80061

suspend fun mySuspendFunction(
    p01: Int, p02: Int, p03: Int, p04: Int, p05: Int, p06: Int, p07: Int, p08: Int, p09: Int, p10: Int,
    p11: Int, p12: Int, p13: Int, p14: Int, p15: Int, p16: Int, p17: Int, p18: Int, p19: Int, p20: Int,
    p21: Int, p22: Int,
) {}

fun myFunction(
    p001: Int, p002: Int, p003: Int, p004: Int, p005: Int, p006: Int, p007: Int, p008: Int, p009: Int, p010: Int,
    p011: Int, p012: Int, p013: Int, p014: Int, p015: Int, p016: Int, p017: Int, p018: Int, p019: Int, p020: Int,
    p021: Int, p022: Int, p023: Int, p024: Int, p025: Int, p026: Int, p027: Int, p028: Int, p029: Int, p030: Int,
    p031: Int, p032: Int, p033: Int, p034: Int, p035: Int, p036: Int, p037: Int, p038: Int, p039: Int, p040: Int,
    p041: Int, p042: Int, p043: Int, p044: Int, p045: Int, p046: Int, p047: Int, p048: Int, p049: Int, p050: Int,
    p051: Int, p052: Int, p053: Int, p054: Int, p055: Int, p056: Int, p057: Int, p058: Int, p059: Int, p060: Int,
    p061: Int, p062: Int, p063: Int, p064: Int, p065: Int, p066: Int, p067: Int, p068: Int, p069: Int, p070: Int,
    p071: Int, p072: Int, p073: Int, p074: Int, p075: Int, p076: Int, p077: Int, p078: Int, p079: Int, p080: Int,
    p081: Int, p082: Int, p083: Int, p084: Int, p085: Int, p086: Int, p087: Int, p088: Int, p089: Int, p090: Int,
    p091: Int, p092: Int, p093: Int, p094: Int, p095: Int, p096: Int, p097: Int, p098: Int, p099: Int, p100: Int,
    p101: Int, p102: Int, p103: Int, p104: Int, p105: Int, p106: Int, p107: Int, p108: Int, p109: Int, p110: Int,
    p111: Int, p112: Int, p113: Int, p114: Int, p115: Int, p116: Int, p117: Int, p118: Int, p119: Int, p120: Int,
    p121: Int, p122: Int, p123: Int, p124: Int, p125: Int, p126: Int, p127: Int, p128: Int, p129: Int, p130: Int,
    p131: Int, p132: Int, p133: Int, p134: Int, p135: Int, p136: Int, p137: Int, p138: Int, p139: Int, p140: Int,
    p141: Int, p142: Int, p143: Int, p144: Int, p145: Int, p146: Int, p147: Int, p148: Int, p149: Int, p150: Int,
    p151: Int, p152: Int, p153: Int, p154: Int, p155: Int, p156: Int, p157: Int, p158: Int, p159: Int, p160: Int,
    p161: Int, p162: Int, p163: Int, p164: Int, p165: Int, p166: Int, p167: Int, p168: Int, p169: Int, p170: Int,
    p171: Int, p172: Int, p173: Int, p174: Int, p175: Int, p176: Int, p177: Int, p178: Int, p179: Int, p180: Int,
    p181: Int, p182: Int, p183: Int, p184: Int, p185: Int, p186: Int, p187: Int, p188: Int, p189: Int, p190: Int,
    p191: Int, p192: Int, p193: Int, p194: Int, p195: Int, p196: Int, p197: Int, p198: Int, p199: Int, p200: Int,
    p201: Int, p202: Int, p203: Int, p204: Int, p205: Int, p206: Int, p207: Int, p208: Int, p209: Int, p210: Int,
    p211: Int, p212: Int, p213: Int, p214: Int, p215: Int, p216: Int, p217: Int, p218: Int, p219: Int, p220: Int,
    p221: Int, p222: Int, p223: Int, p224: Int, p225: Int, p226: Int, p227: Int, p228: Int, p229: Int, p230: Int,
    p231: Int, p232: Int, p233: Int, p234: Int, p235: Int, p236: Int, p237: Int, p238: Int, p239: Int, p240: Int,
    p241: Int, p242: Int, p243: Int, p244: Int, p245: Int, p246: Int, p247: Int, p248: Int, p249: Int, p250: Int,
    p251: Int, p252: Int, p253: Int, p254: Int, p255: Int,
) {}

fun main() {
    <!K_SUSPEND_FUNCTION_TYPE_OF_DANGEROUSLY_LARGE_ARITY!>::mySuspendFunction<!>
    <!FUNCTION_TYPE_OF_TOO_LARGE_ARITY!>::myFunction<!>
}

/* GENERATED_FIR_TAGS: callableReference, functionDeclaration, suspend */
