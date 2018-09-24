/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.mockJDK;

import com.intellij.openapi.util.io.FileUtil;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class GenerateMockJdk {

    private static final String[] ENTRIES = {
            "java/awt/Component.class",
            "java/awt/Container.class",
            "java/awt/Dimension.class",
            "java/awt/event/ActionEvent.class",
            "java/awt/event/ActionListener.class",
            "java/awt/AWTKeyStroke.class",
            "java/awt/Frame.class",
            "java/awt/GridBagConstraints.class",
            "java/awt/Window.class",
            "java/beans/beancontext/BeanContextServiceRevokedListener.class",
            "java/beans/beancontext/BeanContextServicesSupport.class",
            "java/io/BufferedInputStream.class",
            "java/io/BufferedOutputStream.class",
            "java/io/BufferedReader.class",
            "java/io/BufferedWriter.class",
            "java/io/ByteArrayInputStream.class",
            "java/io/ByteArrayOutputStream.class",
            "java/io/Closeable.class",
            "java/io/DataInputStream.class",
            "java/io/EOFException.class",
            "java/io/Externalizable.class",
            "java/io/File.class",
            "java/io/FileInputStream.class",
            "java/io/FilenameFilter.class",
            "java/io/FileFilter.class",
            "java/io/FileNotFoundException.class",
            "java/io/FileOutputStream.class",
            "java/io/FileReader.class",
            "java/io/FileWriter.class",
            "java/io/FilterOutputStream.class",
            "java/io/InputStream.class",
            "java/io/InputStreamReader.class",
            "java/io/IOException.class",
            "java/io/ObjectInput.class",
            "java/io/ObjectInputStream.class",
            "java/io/ObjectOutput.class",
            "java/io/ObjectOutputStream.class",
            "java/io/ObjectStreamException.class",
            "java/io/OutputStream.class",
            "java/io/OutputStreamWriter.class",
            "java/io/PrintStream.class",
            "java/io/RandomAccessFile.class",
            "java/io/Reader.class",
            "java/io/Serializable.class",
            "java/io/Writer.class",
            "java/lang/AbstractStringBuilder.class",
            "java/lang/annotation/Annotation.class",
            "java/lang/annotation/AnnotationFormatError.class",
            "java/lang/annotation/AnnotationTypeMismatchException.class",
            "java/lang/annotation/Documented.class",
            "java/lang/annotation/ElementType.class",
            "java/lang/annotation/IncompleteAnnotationException.class",
            "java/lang/annotation/Inherited.class",
            "java/lang/annotation/Retention.class",
            "java/lang/annotation/RetentionPolicy.class",
            "java/lang/annotation/Target.class",
            "java/lang/Appendable.class",
            "java/lang/AutoCloseable.class",
            "java/lang/ArithmeticException.class",
            "java/lang/ArrayIndexOutOfBoundsException.class",
            "java/lang/ArrayStoreException.class",
            "java/lang/AssertionError.class",
            "java/lang/Boolean.class",
            "java/lang/Byte.class",
            "java/lang/Character.class",
            "java/lang/CharSequence.class",
            "java/lang/Class.class",
            "java/lang/ClassCastException.class",
            "java/lang/ClassLoader.class",
            "java/lang/ClassNotFoundException.class",
            "java/lang/Cloneable.class",
            "java/lang/CloneNotSupportedException.class",
            "java/lang/Comparable.class",
            "java/lang/Deprecated.class",
            "java/lang/Double.class",
            "java/lang/Enum.class",
            "java/lang/Error.class",
            "java/lang/Exception.class",
            "java/lang/Float.class",
            "java/lang/IllegalAccessException.class",
            "java/lang/IllegalArgumentException.class",
            "java/lang/IllegalStateException.class",
            "java/lang/IndexOutOfBoundsException.class",
            "java/lang/Integer.class",
            "java/lang/Iterable.class",
            "java/lang/Long.class",
            "java/lang/Math.class",
            "java/lang/NegativeArraySizeException.class",
            "java/lang/NoSuchFieldException.class",
            "java/lang/NullPointerException.class",
            "java/lang/Number.class",
            "java/lang/NumberFormatException.class",
            "java/lang/Object.class",
            "java/lang/Override.class",
            "java/lang/Readable.class",
            "java/lang/reflect/AccessibleObject.class",
            "java/lang/reflect/AnnotatedElement.class",
            "java/lang/reflect/Array.class",
            "java/lang/reflect/Constructor.class",
            "java/lang/reflect/Field.class",
            "java/lang/reflect/GenericDeclaration.class",
            "java/lang/reflect/Member.class",
            "java/lang/reflect/Method.class",
            "java/lang/reflect/Type.class",
            "java/lang/Runnable.class",
            "java/lang/RuntimeException.class",
            "java/lang/Short.class",
            "java/lang/String.class",
            "java/lang/StringBuffer.class",
            "java/lang/StringBuilder.class",
            "java/lang/SuppressWarnings.class",
            "java/lang/System.class",
            "java/lang/Thread.class",
            "java/lang/ThreadLocal.class",
            "java/lang/Throwable.class",
            "java/lang/UnsupportedOperationException.class",
            "java/lang/Void.class",
            "java/net/ConnectException.class",
            "java/net/DatagramPacket.class",
            "java/net/DatagramSocket.class",
            "java/net/InetAddress.class",
            "java/net/InetAddressImpl.class",
            "java/net/MalformedURLException.class",
            "java/net/ServerSocket.class",
            "java/net/Socket.class",
            "java/net/SocketException.class",
            "java/net/UnknownHostException.class",
            "java/net/URI.class",
            "java/net/URL.class",
            "java/net/URLConnection.class",
            "java/nio/charset/Charset.class",
            "java/rmi/Remote.class",
            "java/rmi/RemoteException.class",
            "java/security/GeneralSecurityException.class",
            "java/security/Identity.class",
            "java/security/NoSuchAlgorithmException.class",
            "java/security/Policy.class",
            "java/security/Provider.class",
            "java/security/Security.class",
            "java/security/Signer.class",
            "java/sql/Array.class",
            "java/sql/BatchUpdateException.class",
            "java/sql/Blob.class",
            "java/sql/CallableStatement.class",
            "java/sql/Clob.class",
            "java/sql/Connection.class",
            "java/sql/DatabaseMetaData.class",
            "java/sql/DataTruncation.class",
            "java/sql/Date.class",
            "java/sql/Driver.class",
            "java/sql/DriverInfo.class",
            "java/sql/DriverManager.class",
            "java/sql/DriverPropertyInfo.class",
            "java/sql/ParameterMetaData.class",
            "java/sql/PreparedStatement.class",
            "java/sql/Ref.class",
            "java/sql/ResultSet.class",
            "java/sql/ResultSetMetaData.class",
            "java/sql/Savepoint.class",
            "java/sql/SQLData.class",
            "java/sql/SQLException.class",
            "java/sql/SQLInput.class",
            "java/sql/SQLOutput.class",
            "java/sql/SQLPermission.class",
            "java/sql/SQLWarning.class",
            "java/sql/Statement.class",
            "java/sql/Struct.class",
            "java/sql/Time.class",
            "java/sql/Timestamp.class",
            "java/sql/Types.class",
            "java/util/AbstractCollection.class",
            "java/util/AbstractList.class",
            "java/util/AbstractMap.class",
            "java/util/AbstractQueue.class",
            "java/util/AbstractSequentialList.class",
            "java/util/AbstractSet.class",
            "java/util/ArrayList.class",
            "java/util/Arrays.class",
            "java/util/Calendar.class",
            "java/util/Collection.class",
            "java/util/Collections.class",
            "java/util/Comparator.class",
            "java/util/concurrent/atomic/AtomicBoolean.class",
            "java/util/concurrent/atomic/AtomicInteger.class",
            "java/util/concurrent/atomic/AtomicIntegerArray.class",
            "java/util/concurrent/atomic/AtomicIntegerFieldUpdater.class",
            "java/util/concurrent/atomic/AtomicLong.class",
            "java/util/concurrent/atomic/AtomicLongArray.class",
            "java/util/concurrent/atomic/AtomicLongFieldUpdater.class",
            "java/util/concurrent/atomic/AtomicMarkableReference.class",
            "java/util/concurrent/atomic/AtomicReference.class",
            "java/util/concurrent/atomic/AtomicReferenceArray.class",
            "java/util/concurrent/atomic/AtomicReferenceFieldUpdater.class",
            "java/util/concurrent/atomic/AtomicStampedReference.class",
            "java/util/concurrent/Callable.class",
            "java/util/concurrent/ConcurrentHashMap.class",
            "java/util/concurrent/locks/AbstractQueuedSynchronizer.class",
            "java/util/concurrent/locks/Condition.class",
            "java/util/concurrent/locks/Lock.class",
            "java/util/concurrent/locks/LockSupport.class",
            "java/util/concurrent/locks/ReadWriteLock.class",
            "java/util/concurrent/locks/ReentrantLock.class",
            "java/util/concurrent/locks/ReentrantReadWriteLock.class",
            "java/util/concurrent/TimeUnit.class",
            "java/util/ConcurrentModificationException.class",
            "java/util/Date.class",
            "java/util/Enumeration.class",
            "java/util/EventListener.class",
            "java/util/GregorianCalendar.class",
            "java/util/HashMap.class",
            "java/util/HashSet.class",
            "java/util/IdentityHashMap.class",
            "java/util/Iterator.class",
            "java/util/LinkedHashSet.class",
            "java/util/List.class",
            "java/util/ListIterator.class",
            "java/util/Locale.class",
            "java/util/Map.class",
            "java/util/NoSuchElementException.class",
            "java/util/Properties.class",
            "java/util/Random.class",
            "java/util/RandomAccess.class",
            "java/util/regex/Pattern.class",
            "java/util/Objects.class",
            "java/util/Set.class",
            "java/util/SortedMap.class",
            "java/util/SortedSet.class",
            "java/util/TimeZone.class",
            "java/util/TreeMap.class",
            "java/util/TreeSet.class",
            "javax/swing/AbstractButton.class",
            "javax/swing/Icon.class",
            "javax/swing/JButton.class",
            "javax/swing/JComponent.class",
            "javax/swing/JDialog.class",
            "javax/swing/JFrame.class",
            "javax/swing/JLabel.class",
            "javax/swing/JPanel.class",
            "javax/swing/JScrollPane.class",
            "javax/swing/JTable.class",
            "javax/swing/SwingConstants.class",
            "javax/swing/SwingUtilities.class",
            "javax/swing/table/TableModel.class",
            "sun/util/calendar/BaseCalendar.class",
            "META-INF/",
            "META-INF/MANIFEST.MF",
    };



    private static void generateFilteredJar(
            String jdkPath,
            String childName,
            File target,
            Set<String> entryNamesToInclude,
            boolean assertAllFound
    ) throws IOException {
        File source = new File(jdkPath, childName);
        if (!source.exists()) {
            throw new AssertionError(source + " doesn't exist");
        }
        JarFile sourceJar = new JarFile(source);
        JarOutputStream targetJar = new JarOutputStream(new FileOutputStream(target));

        Set<String> foundEntries = new HashSet<>();

        List<JarEntry> sourceList = Collections.list(sourceJar.entries());
        for (JarEntry entry : sourceList) {
            // For Map$Entry.class we want to check Map.class presense
            String topLevelClassFile = entry.getName().replaceAll("\\$.+\\.class$", ".class");

            if (entryNamesToInclude.contains(topLevelClassFile) && foundEntries.add(entry.getName())) {
                targetJar.putNextEntry(new ZipEntry(entry.getName()));
                FileUtil.copy(sourceJar.getInputStream(entry), targetJar);
            }
        }

        targetJar.close();
        sourceJar.close();

        if (assertAllFound) {
            Set<String> notFound = new HashSet<>(entryNamesToInclude);
            notFound.removeAll(foundEntries);
            if (!notFound.isEmpty()) {
                System.err.println("Not found:");
                for (String entry : notFound) {
                    System.err.println(entry);
                }
                throw new AssertionError("See errors above");
            }
        }
    }

    private static Set<String> getSourceFileEntries() {
        Set<String> entrySet = new HashSet<>();
        for (String entry : ENTRIES) {
            String javaFile = // "src/" + (in jdk 1.7 under Mac there is no src folder in src.zip)
                    entry.replaceAll(".class$", ".java");
            entrySet.add(javaFile);
        }
        return entrySet;
    }

    private static Set<String> getClassFileEntries() {
        return new HashSet<>(Arrays.asList(ENTRIES));
    }

    public static void main(String[] args) throws IOException {
        String openjdk7Path = System.getProperty("openjdk7");
        if (openjdk7Path == null) {
            throw new AssertionError("Provide path to openJDK 7 in VM options: \"-Dopenjdk7=...\"");
        }

        Set<String> sourceFileEntries = getSourceFileEntries();
        try (JarFile sourceJar = new JarFile(new File(openjdk7Path, "src.zip"))) {
            InputStream stream = sourceJar.getInputStream(sourceJar.getEntry(sourceFileEntries.iterator().next()));
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                boolean foundOpenJDKLicense = false;
                while (true) {
                    String line = reader.readLine();
                    if (line == null) break;
                    if (line.contains("\"Classpath\" exception")) {
                        foundOpenJDKLicense = true;
                    }
                }

                if (!foundOpenJDKLicense) {
                    System.out.println("rt.jar and src.zip should point to OpenJDK installation");
                    System.exit(1);
                }
            }
        }

        generateFilteredJar(
                openjdk7Path,
                "jre/lib/rt.jar",
                new File("compiler/testData/mockJDK/jre/lib/rt.jar"),
                getClassFileEntries(),
                true);
        generateFilteredJar(
                openjdk7Path    ,
                "src.zip",
                new File("compiler/testData/mockJDK/src.zip"),
                sourceFileEntries,
                false);
    }

    private GenerateMockJdk() {
    }
}
