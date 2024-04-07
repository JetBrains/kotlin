// WITH_STDLIB
// IGNORE_BACKEND: JVM

import kotlin.coroutines.*

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

fun interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

inline fun <T, R> Flow<T>.transform(
    crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = object : Flow<R> {
    override suspend fun collect(collector: FlowCollector<R>) {
        this@transform.collect a@{ value ->
            return@a collector.transform(value)
        }
    }
}

public inline fun <T, R: Any> Flow<T>.mapNotNull(crossinline transform: suspend (value: T) -> R?): Flow<R> = transform { value ->
    val transformed = transform(value) ?: return@transform
    return@transform emit(transformed)
}

internal fun Flow<List<Data>>.aggregate(aggregation: (List<Double>) -> Double): Flow<Data> = mapNotNull {
    Data(
        p1	=	aggregation(it.mapNotNull { it.	p1	}),
        p2	=	aggregation(it.mapNotNull { it.	p2	}),
        p3	=	aggregation(it.mapNotNull { it.	p3	}),
        p4	=	aggregation(it.mapNotNull { it.	p4	}),
        p5	=	aggregation(it.mapNotNull { it.	p5	}),
        p6	=	aggregation(it.mapNotNull { it.	p6	}),
        p7	=	aggregation(it.mapNotNull { it.	p7	}),
        p8	=	aggregation(it.mapNotNull { it.	p8	}),
        p9	=	aggregation(it.mapNotNull { it.	p9	}),
        p10	=	aggregation(it.mapNotNull { it.	p10	}),
        p11	=	aggregation(it.mapNotNull { it.	p11	}),
        p12	=	aggregation(it.mapNotNull { it.	p12	}),
        p13	=	aggregation(it.mapNotNull { it.	p13	}),
        p14	=	aggregation(it.mapNotNull { it.	p14	}),
        p15	=	aggregation(it.mapNotNull { it.	p15	}),
        p16	=	aggregation(it.mapNotNull { it.	p16	}),
        p17	=	aggregation(it.mapNotNull { it.	p17	}),
        p18	=	aggregation(it.mapNotNull { it.	p18	}),
        p19	=	aggregation(it.mapNotNull { it.	p19	}),
        p20	=	aggregation(it.mapNotNull { it.	p20	}),
        p21	=	aggregation(it.mapNotNull { it.	p21	}),
        p22	=	aggregation(it.mapNotNull { it.	p22	}),
        p23	=	aggregation(it.mapNotNull { it.	p23	}),
        p24	=	aggregation(it.mapNotNull { it.	p24	}),
        p25	=	aggregation(it.mapNotNull { it.	p25	}),
        p26	=	aggregation(it.mapNotNull { it.	p26	}),
        p27	=	aggregation(it.mapNotNull { it.	p27	}),
        p28	=	aggregation(it.mapNotNull { it.	p28	}),
        p29	=	aggregation(it.mapNotNull { it.	p29	}),
        p30	=	aggregation(it.mapNotNull { it.	p30	}),
        p31	=	aggregation(it.mapNotNull { it.	p31	}),
        p32	=	aggregation(it.mapNotNull { it.	p32	}),
        p33	=	aggregation(it.mapNotNull { it.	p33	}),
        p34	=	aggregation(it.mapNotNull { it.	p34	}),
        p35	=	aggregation(it.mapNotNull { it.	p35	}),
        p36	=	aggregation(it.mapNotNull { it.	p36	}),
        p37	=	aggregation(it.mapNotNull { it.	p37	}),
        p38	=	aggregation(it.mapNotNull { it.	p38	}),
        p39	=	aggregation(it.mapNotNull { it.	p39	}),
        p40	=	aggregation(it.mapNotNull { it.	p40	}),
        p41	=	aggregation(it.mapNotNull { it.	p41	}),
        p42	=	aggregation(it.mapNotNull { it.	p42	}),
        p43	=	aggregation(it.mapNotNull { it.	p43	}),
        p44	=	aggregation(it.mapNotNull { it.	p44	}),
        p45	=	aggregation(it.mapNotNull { it.	p45	}),
        p46	=	aggregation(it.mapNotNull { it.	p46	}),
        p47	=	aggregation(it.mapNotNull { it.	p47	}),
        p48	=	aggregation(it.mapNotNull { it.	p48	}),
        p49	=	aggregation(it.mapNotNull { it.	p49	}),
        p50	=	aggregation(it.mapNotNull { it.	p50	}),
        p51	=	aggregation(it.mapNotNull { it.	p51	}),
        p52	=	aggregation(it.mapNotNull { it.	p52	}),
        p53	=	aggregation(it.mapNotNull { it.	p53	}),
        p54	=	aggregation(it.mapNotNull { it.	p54	}),
        p55	=	aggregation(it.mapNotNull { it.	p55	}),
        p56	=	aggregation(it.mapNotNull { it.	p56	}),
        p57	=	aggregation(it.mapNotNull { it.	p57	}),
        p58	=	aggregation(it.mapNotNull { it.	p58	}),
        p59	=	aggregation(it.mapNotNull { it.	p59	}),
        p60	=	aggregation(it.mapNotNull { it.	p60	}),
        p61	=	aggregation(it.mapNotNull { it.	p61	}),
        p62	=	aggregation(it.mapNotNull { it.	p62	}),
        p63	=	aggregation(it.mapNotNull { it.	p63	}),
        p64	=	aggregation(it.mapNotNull { it.	p64	}),
        p65	=	aggregation(it.mapNotNull { it.	p65	}),
        p66	=	aggregation(it.mapNotNull { it.	p66	}),
        p67	=	aggregation(it.mapNotNull { it.	p67	}),
        p68	=	aggregation(it.mapNotNull { it.	p68	}),
        p69	=	aggregation(it.mapNotNull { it.	p69	}),
        p70	=	aggregation(it.mapNotNull { it.	p70	}),
        p71	=	aggregation(it.mapNotNull { it.	p71	}),
        p72	=	aggregation(it.mapNotNull { it.	p72	}),
        p73	=	aggregation(it.mapNotNull { it.	p73	}),
        p74	=	aggregation(it.mapNotNull { it.	p74	}),
        p75	=	aggregation(it.mapNotNull { it.	p75	}),
        p76	=	aggregation(it.mapNotNull { it.	p76	}),
        p77	=	aggregation(it.mapNotNull { it.	p77	}),
        p78	=	aggregation(it.mapNotNull { it.	p78	}),
        p79	=	aggregation(it.mapNotNull { it.	p79	}),
        p80	=	aggregation(it.mapNotNull { it.	p80	}),
        p81	=	aggregation(it.mapNotNull { it.	p81	}),
        p82	=	aggregation(it.mapNotNull { it.	p82	}),
        p83	=	aggregation(it.mapNotNull { it.	p83	}),
        p84	=	aggregation(it.mapNotNull { it.	p84	}),
        p85	=	aggregation(it.mapNotNull { it.	p85	}),
        p86	=	aggregation(it.mapNotNull { it.	p86	}),
    )
}

data class Data(
    val	p1	: Double?,
    val	p2	: Double?,
    val	p3	: Double?,
    val	p4	: Double?,
    val	p5	: Double?,
    val	p6	: Double?,
    val	p7	: Double?,
    val	p8	: Double?,
    val	p9	: Double?,
    val	p10	: Double?,
    val	p11	: Double?,
    val	p12	: Double?,
    val	p13	: Double?,
    val	p14	: Double?,
    val	p15	: Double?,
    val	p16	: Double?,
    val	p17	: Double?,
    val	p18	: Double?,
    val	p19	: Double?,
    val	p20	: Double?,
    val	p21	: Double?,
    val	p22	: Double?,
    val	p23	: Double?,
    val	p24	: Double?,
    val	p25	: Double?,
    val	p26	: Double?,
    val	p27	: Double?,
    val	p28	: Double?,
    val	p29	: Double?,
    val	p30	: Double?,
    val	p31	: Double?,
    val	p32	: Double?,
    val	p33	: Double?,
    val	p34	: Double?,
    val	p35	: Double?,
    val	p36	: Double?,
    val	p37	: Double?,
    val	p38	: Double?,
    val	p39	: Double?,
    val	p40	: Double?,
    val	p41	: Double?,
    val	p42	: Double?,
    val	p43	: Double?,
    val	p44	: Double?,
    val	p45	: Double?,
    val	p46	: Double?,
    val	p47	: Double?,
    val	p48	: Double?,
    val	p49	: Double?,
    val	p50	: Double?,
    val	p51	: Double?,
    val	p52	: Double?,
    val	p53	: Double?,
    val	p54	: Double?,
    val	p55	: Double?,
    val	p56	: Double?,
    val	p57	: Double?,
    val	p58	: Double?,
    val	p59	: Double?,
    val	p60	: Double?,
    val	p61	: Double?,
    val	p62	: Double?,
    val	p63	: Double?,
    val	p64	: Double?,
    val	p65	: Double?,
    val	p66	: Double?,
    val	p67	: Double?,
    val	p68	: Double?,
    val	p69	: Double?,
    val	p70	: Double?,
    val	p71	: Double?,
    val	p72	: Double?,
    val	p73	: Double?,
    val	p74	: Double?,
    val	p75	: Double?,
    val	p76	: Double?,
    val	p77	: Double?,
    val	p78	: Double?,
    val	p79	: Double?,
    val	p80	: Double?,
    val	p81	: Double?,
    val	p82	: Double?,
    val	p83	: Double?,
    val	p84	: Double?,
    val	p85	: Double?,
    val	p86	: Double?,
)

fun box(): String {
    object : Flow<List<Data>> {
        override suspend fun collect(collector: FlowCollector<List<Data>>) {
        }
    }.aggregate { 1.0 }
    return "OK"
}