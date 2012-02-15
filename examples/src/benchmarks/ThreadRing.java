/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The Computer Language Benchmarks Game
 * http://shootout.alioth.debian.org/
 *
 * contributed by Fabien Le Floc'h
 *
 * Java implementation of thread-ring benchmark. Best performance is achieved with
 * MAX_THREAD=1 as the thread-ring test is bested with only 1 os thread.
 * This implementation shows using a simple thread pool solves the thread context
 * switch issue.
 */

import java.util.concurrent.*;

public class ThreadRing {
    private static final int MAX_NODES = 503;
    private static final int MAX_THREADS = 503;

    private ExecutorService executor;
    private final int N;

    static final CountDownLatch cdl = new CountDownLatch(1);

    public static void main(String[] args) throws InterruptedException {
        long start = System.currentTimeMillis();
        int n = 5000000;
        try {
            n = Integer.parseInt(args[0]);
        } catch (Exception e) {}
        ThreadRing ring = new ThreadRing(n);
        Node node = ring.start(MAX_NODES);
        node.sendMessage(new TokenMessage(1,0));
        cdl.await();

        long total = System.currentTimeMillis() - start;
        System.out.println("[ThreadRing-" + System.getProperty("project.name")+ " Benchmark Result: " + total + "]");
    }

    public ThreadRing(int n) {
        N = n;
    }

    public Node start(int n) {
        Node[] nodes = spawnNodes(n);
        connectNodes(n, nodes);
        return nodes[0];
    }

    private Node[] spawnNodes(int n) {
        executor = Executors.newFixedThreadPool(MAX_THREADS);
        Node[] nodes = new Node[n+1];
        for (int i = 0; i < n ; i++) {
            nodes[i] = new Node(i+1, null);
        }
        return nodes;
    }

    public void connectNodes(int n, Node[] nodes) {
        nodes[n] = nodes[0];
        for (int i=0; i<n; i++) {
            nodes[i].connect(nodes[i+1]);
        }
    }

    private static class TokenMessage {
        private int nodeId;
        private volatile int value;
        private boolean isStop;

        public TokenMessage(int nodeId, int value) {
            this.nodeId = nodeId;
            this.value = value;
        }

        public TokenMessage(int nodeId, int value, boolean isStop) {
            this.nodeId = nodeId;
            this.value = value;
            this.isStop = isStop;
        }
    }

    private class Node implements Runnable {
        private int nodeId;
        private Node nextNode;
        private BlockingQueue<TokenMessage> queue = new LinkedBlockingQueue<TokenMessage>();
        private boolean isActive = false;
        private int counter;

        public Node(int id, Node nextNode) {
            this.nodeId = id;
            this.nextNode = nextNode;
            this.counter = 0;
        }

        public void connect(Node node) {
            this.nextNode = node;
            isActive = true;
        }

        public void sendMessage(TokenMessage m) {
            queue.add(m);
            executor.execute(this);
        }


        public void run() {
            if (isActive) {
                try {
                    TokenMessage m = queue.take();
                    if (m.isStop) {
                        int nextValue = m.value+1;
                        if (nextValue == MAX_NODES) {
//                            System.out.println("last one");
                            executor.shutdown();
                            cdl.countDown();
                        } else {
                            m.value = nextValue;
                            nextNode.sendMessage(m);
                        }
                        isActive = false;
//                        System.out.println("ending node "+nodeId);
                    } else {
                        if (m.value == N) {
                            System.out.println(nodeId);
                            nextNode.sendMessage(new TokenMessage(nodeId, 0, true));
                        } else {
                            m.value = m.value + 1;
                            nextNode.sendMessage(m);
                        }
                    }
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }
}
