import jdk.jfr.consumer.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Aggregates JFR stacks into the collapsed format: "root;...;leaf count".
 * Frames are "Class.method" (no line numbers). Only threads matching the regex are kept.
 *
 * usage: java JfrAgg.java <in.jfr> <out-prefix> [thread-regex]
 * writes <out-prefix>.cpu.collapsed (jdk.ExecutionSample counts) and <out-prefix>.alloc.collapsed
 * (jdk.ObjectAllocationSample weights in KB, leaf frame = allocated class name).
 */
public class JfrAgg {
    public static void main(String[] args) throws IOException {
        Path in = Paths.get(args[0]);
        String prefix = args[1];
        Pattern threads = Pattern.compile(args.length > 2 ? args[2] : ".*");
        Map<String, long[]> cpu = new HashMap<>();
        Map<String, long[]> alloc = new HashMap<>();
        long total = 0, kept = 0, truncated = 0, allocEvents = 0;
        try (RecordingFile rf = new RecordingFile(in)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent e;
                try {
                    e = rf.readEvent();
                } catch (IOException ex) {
                    System.err.println("stopped at a broken chunk: " + ex.getMessage());
                    break;
                }
                String type = e.getEventType().getName();
                boolean isCpu = type.equals("jdk.ExecutionSample");
                boolean isAlloc = type.equals("jdk.ObjectAllocationSample");
                if (!isCpu && !isAlloc) continue;
                RecordedThread t = isCpu ? e.getThread("sampledThread") : e.getThread("eventThread");
                String tn = t == null ? "?" : t.getJavaName();
                if (tn == null || !threads.matcher(tn).find()) continue;
                RecordedStackTrace st = e.getStackTrace();
                if (st == null) continue;
                if (st.isTruncated()) truncated++;
                StringBuilder sb = new StringBuilder();
                List<RecordedFrame> frames = st.getFrames();
                for (int i = frames.size() - 1; i >= 0; i--) {
                    RecordedMethod m = frames.get(i).getMethod();
                    if (sb.length() > 0) sb.append(';');
                    sb.append(m.getType().getName()).append('.').append(m.getName());
                }
                if (isCpu) {
                    total++;
                    kept++;
                    cpu.computeIfAbsent(sb.toString(), k -> new long[1])[0]++;
                } else {
                    allocEvents++;
                    RecordedClass c = e.getClass("objectClass");
                    sb.append(";[alloc] ").append(c == null ? "?" : c.getName());
                    alloc.computeIfAbsent(sb.toString(), k -> new long[1])[0] += e.getLong("weight") / 1024;
                }
            }
        }
        write(cpu, prefix + ".cpu.collapsed");
        write(alloc, prefix + ".alloc.collapsed");
        System.err.println("cpuSamples=" + kept + " allocSamples=" + allocEvents + " truncated=" + truncated + " uniqueCpu=" + cpu.size());
    }

    private static void write(Map<String, long[]> counts, String file) throws IOException {
        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(Paths.get(file), StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {
            for (Map.Entry<String, long[]> en : counts.entrySet()) {
                w.print(en.getKey());
                w.print(' ');
                w.println(en.getValue()[0]);
            }
        }
    }
}
