/*
The Computer Language Benchmarks Game
http://shootout.alioth.debian.org/

Based on C# entry by Isaac Gouy
contributed by Jarkko Miettinen
Parallel by The Anh Tran
*/

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.concurrent.CyclicBarrier;

public class SpectralNorm
{
    private static final NumberFormat formatter = new DecimalFormat ("#.000000000");

    public static void main (String[] args)
    {
        int n = 5500;
        if (args.length > 0) n = Integer.parseInt (args[0]);

        final long millis = System.currentTimeMillis();
        System.out.println (formatter.format (spectralnormGame (n)) );
        long total = System.currentTimeMillis() - millis;
        System.out.println("[SpectralNorm-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
    }


    private static final double spectralnormGame (int n)
    {
        // create unit vector
        double[] u = new double[n];
        double[] v = new double[n];
        double[] tmp = new double[n];

        for (int i = 0; i < n; i++)
            u[i] = 1.0;

        // get available processor, then set up syn object
        int nthread = Runtime.getRuntime ().availableProcessors ();
        Approximate.barrier = new CyclicBarrier (nthread);

        int chunk = n / nthread;
        Approximate[] ap = new Approximate[nthread];

        for (int i = 0; i < nthread; i++)
        {
            int r1 = i * chunk;
            int r2 = (i < (nthread -1)) ? r1 + chunk : n;

            ap[i] = new Approximate (u, v, tmp, r1, r2);
        }


        double vBv = 0, vv = 0;
        for (int i = 0; i < nthread; i++)
        {
            try
            {
                ap[i].join ();

                vBv += ap[i].m_vBv;
                vv += ap[i].m_vv;
            }
            catch (Exception e)
            {
                e.printStackTrace ();
            }
        }

        return Math.sqrt (vBv/vv);
    }


    private static class Approximate extends Thread
    {
        private static CyclicBarrier barrier;

        private double[] _u;
        private double[] _v;
        private double[] _tmp;

        private int range_begin, range_end;
        private double m_vBv = 0, m_vv = 0;


        public Approximate (double[] u, double[] v, double[] tmp, int rbegin, int rend)
        {
            super ();

            _u = u;
            _v = v;
            _tmp = tmp;

            range_begin = rbegin;
            range_end = rend;

            start ();
        }

        public void run ()
        {
            // 20 steps of the power method
            for (int i = 0; i < 10; i++)
            {
                MultiplyAtAv (_u, _tmp, _v);
                MultiplyAtAv (_v, _tmp, _u);
            }

            for (int i = range_begin; i < range_end; i++)
            {
                m_vBv += _u[i] * _v[i];
                m_vv  += _v[i] * _v[i];
            }
        }

        /* return element i,j of infinite matrix A */
        private final static double eval_A (int i, int j)
        {
            int div = ( ((i+j) * (i+j+1) >>> 1) +i+1 );
            return 1.0 / div;
        }

        /* multiply vector v by matrix A, each thread evaluate its range only */
        private final void MultiplyAv (final double[] v, double[] Av)
        {
            for (int i = range_begin; i < range_end; i++)
            {
                double sum = 0;
                for (int j = 0; j < v.length; j++)
                    sum += eval_A (i, j) * v[j];

                Av[i] = sum;
            }
        }

        /* multiply vector v by matrix A transposed */
        private final void MultiplyAtv (final double[] v, double[] Atv)
        {
            for (int i = range_begin; i < range_end; i++)
            {
                double sum = 0;
                for (int j = 0; j < v.length; j++)
                    sum += eval_A (j, i) * v[j];

                Atv[i] = sum;
            }
        }

        /* multiply vector v by matrix A and then by matrix A transposed */
        private final void MultiplyAtAv (final double[] v, double[] tmp, double[] AtAv)
        {
            try
            {
                MultiplyAv (v, tmp);
                // all thread must syn at completion
                barrier.await ();
                MultiplyAtv (tmp, AtAv);
                // all thread must syn at completion
                barrier.await ();
            }
            catch (Exception e)
            {
                e.printStackTrace ();
            }
        }
    }
}