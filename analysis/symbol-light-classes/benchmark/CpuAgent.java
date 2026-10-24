import java.io.*;
import java.lang.management.*;

/** Appends "<label> cpu=<s> wall=<s> gc=<s>" of the JVM to the file given as the agent argument "<file>;<label>" at exit. */
public class CpuAgent {
    public static void premain(String args) {
        final long start = System.currentTimeMillis();
        final String[] parts = args.split(";", 2);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
                long cpu = ((com.sun.management.OperatingSystemMXBean) os).getProcessCpuTime();
                long gc = 0;
                for (GarbageCollectorMXBean b : ManagementFactory.getGarbageCollectorMXBeans()) gc += Math.max(0, b.getCollectionTime());
                try (FileWriter w = new FileWriter(parts[0], true)) {
                    w.write(String.format("%s cpu=%.1f wall=%.1f gc=%.1f%n", parts.length > 1 ? parts[1] : "-", cpu / 1e9, (System.currentTimeMillis() - start) / 1e3, gc / 1e3));
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }));
    }
}
