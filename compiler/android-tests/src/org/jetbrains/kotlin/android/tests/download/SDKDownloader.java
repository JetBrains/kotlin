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

package org.jetbrains.kotlin.android.tests.download;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.kotlin.android.tests.PathManager;
import org.jetbrains.kotlin.android.tests.run.RunUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class SDKDownloader {
    private final String platformZipPath;
    private final String armImage;
    private final String x86Image;
    private final String platformToolsZipPath;
    private final String skdToolsZipPath;
    private final String buildToolsZipPath;
    private final String gradleZipPath;

    private final PathManager pathManager;

    private static final String PLATFORM_TOOLS = "28.0.1";
    private static final String SDK_TOOLS = "25.2.5";
    public static final String BUILD_TOOLS = "28.0.3";
    private static final int ANDROID_VERSION = 19;
    public static final String GRADLE_VERSION = "4.6";


    public SDKDownloader(PathManager pathManager) {
        this.pathManager = pathManager;
        platformZipPath = pathManager.getRootForDownload() + "/platform" + ANDROID_VERSION + ".zip";
        armImage = pathManager.getRootForDownload() + "/arm-image.zip";
        x86Image = pathManager.getRootForDownload() + "/x86-image.zip";
        platformToolsZipPath = pathManager.getRootForDownload() + "/platform-tools" + PLATFORM_TOOLS + ".zip";
        skdToolsZipPath = pathManager.getRootForDownload() + "/tools" + SDK_TOOLS + ".zip";
        buildToolsZipPath = pathManager.getRootForDownload() + "/build-tools" + BUILD_TOOLS + ".zip";
        gradleZipPath = pathManager.getRootForDownload() + "/gradle" + GRADLE_VERSION + ".zip";
    }

    public void downloadPlatform() {
        download("https://dl-ssl.google.com/android/repository/android-" + ANDROID_VERSION + "_r04.zip", platformZipPath);  //Same for all platforms
    }

    private void downloadAbi() {
        download("https://dl.google.com/android/repository/sys-img/android/armeabi-v7a-" + ANDROID_VERSION + "_r05.zip", armImage);  //Same for all platforms
        download("https://dl.google.com/android/repository/sys-img/android/x86-" + ANDROID_VERSION + "_r06.zip", x86Image);  //Same for all platforms
    }

    public void downloadPlatformTools() {
        download(getDownloadUrl("https://dl-ssl.google.com/android/repository/platform-tools_r" + PLATFORM_TOOLS), platformToolsZipPath);
    }

    public void downloadSdkTools() {
        download(getDownloadUrl("https://dl.google.com/android/repository/tools_r" + SDK_TOOLS), skdToolsZipPath);
    }

    public void downloadBuildTools() {
        download(getDownloadUrl("https://dl.google.com/android/repository/build-tools_r" + BUILD_TOOLS), buildToolsZipPath);
    }

    public void downloadGradle() {
        download("https://services.gradle.org/distributions/gradle-" + GRADLE_VERSION + "-bin.zip", gradleZipPath);
    }

    private static String getDownloadUrl(String prefix) {
        String suffix;
        if (SystemInfo.isWindows) {
            suffix = "-windows.zip";
        }
        else if (SystemInfo.isMac) {
            suffix = "-macosx.zip";
        }
        else if (SystemInfo.isUnix) {
            suffix = "-linux.zip";
        }
        else {
            throw new IllegalStateException("Your operating system doesn't supported yet.");
        }
        return prefix + suffix;
    }

    public void downloadAll() {
        downloadSdkTools();
        downloadAbi();
        downloadPlatform();
        downloadPlatformTools();
        downloadBuildTools();
        downloadGradle();
    }


    public void unzipAll() {
        String androidSdkRoot = pathManager.getAndroidSdkRoot();
        unzip(platformZipPath, pathManager.getPlatformFolderInAndroidSdk());
        new File(pathManager.getPlatformFolderInAndroidSdk() + "/android-4.4.2").renameTo(new File(pathManager.getPlatformFolderInAndroidSdk() + "/android-" + ANDROID_VERSION));

        unzip(armImage, androidSdkRoot + "/system-images/android-" + ANDROID_VERSION + "/default/");
        unzip(x86Image, androidSdkRoot + "/system-images/android-" + ANDROID_VERSION + "/default/");

        unzip(platformToolsZipPath, androidSdkRoot);
        unzip(skdToolsZipPath, androidSdkRoot);

        unzip(gradleZipPath, pathManager.getDependenciesRoot());

        //BUILD TOOLS
        String buildTools = androidSdkRoot + "/build-tools/";
        String buildToolsFolder = buildTools + BUILD_TOOLS + "/";
        new File(buildToolsFolder).delete();
        unzip(buildToolsZipPath, buildTools);
        new File(buildTools + "/android-9").renameTo(new File(buildToolsFolder));
    }

    public void deleteAll() {
        delete(platformZipPath);
        delete(platformToolsZipPath);
        delete(skdToolsZipPath);
        delete(buildToolsZipPath);
        delete(armImage);
        delete(x86Image);
        delete(gradleZipPath);
    }

    private static void download(String urlString, String output) {
        System.out.println("Start downloading: " + urlString + " to " + output);
        OutputStream outStream = null;
        URLConnection urlConnection;

        InputStream is;
        try {
            URL Url;
            byte[] buf;
            int read;
            //int written = 0;
            Url = new URL(urlString);

            File outputFile = new File(output);
            outputFile.getParentFile().mkdirs();
            if (outputFile.exists()) {
                System.out.println("File was already downloaded: " + output);
                return;
            }
            outputFile.createNewFile();
            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outStream = new BufferedOutputStream(outputStream);

            urlConnection = Url.openConnection();
            is = urlConnection.getInputStream();
            buf = new byte[1024];
            while ((read = is.read(buf)) != -1) {
                outStream.write(buf, 0, read);
                //written += read;
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            RunUtils.close(outStream);
        }
        System.out.println("Finish downloading: " + urlString + " to " + output);
    }

    protected void unzip(String pathToFile, String outputFolder) {
        System.out.println("Start unzipping: " + pathToFile + " to " + outputFolder);
        String pathToUnzip;
        if (outputFolder.equals(pathManager.getPlatformFolderInAndroidSdk())) {
            pathToUnzip = outputFolder;
        }
        else {
            pathToUnzip = outputFolder + "/" + FileUtil.getNameWithoutExtension(new File(pathToFile));
        }
        if (new File(pathToUnzip).listFiles() != null) {
            System.out.println("File was already unzipped: " + pathToFile);
            return;
        }
        try {
            byte[] buf = new byte[1024];
            ZipEntry zipEntry = null;
            try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(pathToFile))) {
                zipEntry = zipInputStream.getNextEntry();
                while (zipEntry != null) {
                    String entryName = zipEntry.getName();
                    int n;
                    File outputFile = new File(outputFolder + "/" + entryName);

                    if (zipEntry.isDirectory()) {
                        outputFile.mkdirs();
                        zipInputStream.closeEntry();
                        zipEntry = zipInputStream.getNextEntry();
                        continue;
                    }
                    else {
                        File parentFile = outputFile.getParentFile();
                        if (parentFile != null && !parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        outputFile.createNewFile();
                    }

                    try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile)) {
                        while ((n = zipInputStream.read(buf, 0, 1024)) > -1) {
                            fileOutputStream.write(buf, 0, n);
                        }
                    }
                    zipInputStream.closeEntry();
                    zipEntry = zipInputStream.getNextEntry();
                }
            }
            catch (IOException e) {
                if (zipEntry != null) {
                    System.err.println("Entry name: " + zipEntry.getName());
                }
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finish unzipping: " + pathToFile + " to " + outputFolder);
    }

    private static void delete(String filePath) {
        new File(filePath).delete();
    }
}

