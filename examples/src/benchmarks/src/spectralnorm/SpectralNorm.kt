package spectralnorm_kotlin

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CyclicBarrier;

val formatter = DecimalFormat ("#.000000000");

fun main (args : Array<String>) {
    var n = 5500
    if (args.size > 0)
        n = Integer.parseInt (args[0]);

    val millis = System.currentTimeMillis()
    System.out?.println (formatter.format (spectralnormGame (n)) )
    val total = System.currentTimeMillis() - millis;
    System.out?.println("[SpectralNorm-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
}

fun spectralnormGame(n: Int) : Double {
    val u = DoubleArray(n)
    val v = DoubleArray(n)
    val tmp = DoubleArray(n)

    for(i in u.indices) {
        u[i] = 1.0
    }

    val nthread = Runtime.getRuntime ().sure().availableProcessors ();
    barrier = CyclicBarrier (nthread);

    val chunk = n / nthread
    val ap = Array<Approximate>(nthread,{
        val r1 = it * chunk;
        val r2 = if(it < (nthread -1)) r1 + chunk else nthread;

        Approximate (u, v, tmp, r1, r2)
    })

    var vBv = 0.toDouble()
    var vv = 0.toDouble();
    for (i in 0..nthread-1) {
        try {
            ap[i].join ();

            vBv += ap[i].m_vBv;
            vv += ap[i].m_vv;
        }
        catch (e: Exception )
        {
            e.printStackTrace ();
        }
    }

    return Math.sqrt (vBv/vv);
}

fun eval_A (i: Int, j: Int) = 1.0 / ( ((i+j) * (i+j+1) shr 1) +i+1 )

var barrier : CyclicBarrier? = null

class Approximate(val u: DoubleArray, val v: DoubleArray, val _tmp: DoubleArray, val rbegin: Int, val rend: Int) : Thread() {
    class object {
    }

    var m_vBv = 0.0
    var m_vv  = 0.0

    {
        start()
    }

    override fun run () {
        for(i in 0..10) {
            MultiplyAtAv (u, _tmp, v);
            MultiplyAtAv (v, _tmp, u);
        }

        for(i in rbegin..rend) {
            m_vBv += u[i] * v[i];
            m_vv  += v[i] * v[i];
        }
    }

/* multiply vector v by matrix A, each thread evaluate its range only */
    fun MultiplyAv (v: DoubleArray, Av: DoubleArray)
    {
        for (i in rbegin..rend)
        {
            var sum = 0.0;
            for (j in v.indices)
                sum += eval_A (i, j) * v[j];

            Av[i] = sum;
        }
    }

/* multiply vector v by matrix A transposed */
    fun MultiplyAtv (v: DoubleArray, Atv: DoubleArray)
    {
        for (i in rbegin..rend)
        {
            var sum = 0.toDouble()
            for (j in v.indices)
                sum += eval_A (j, i) * v[j];

            Atv[i] = sum;
        }
    }

/* multiply vector v by matrix A and then by matrix A transposed */
    fun MultiplyAtAv (v: DoubleArray, tmp: DoubleArray, AtAv: DoubleArray)
    {
        try
        {
            MultiplyAv (v, tmp);
            // all thread must syn at completion
            barrier?.await ();
            MultiplyAtv (tmp, AtAv);
            // all thread must syn at completion
            barrier?.await ();
        }
        catch (e: Exception)
        {
            e.printStackTrace ();
        }
    }
}