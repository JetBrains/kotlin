/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.compiler.download;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.compiler.PathManager;
import org.jetbrains.jet.compiler.run.RunUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class SDKDownloader {
    private final String platformZipPath;
    private final String systemImages;
    private final String platformToolsZipPath;
    private final String toolsZipPath;
    private final String antZipPath;

    private final PathManager pathManager;

    public SDKDownloader(PathManager pathManager) {
        this.pathManager = pathManager;
        platformZipPath = pathManager.getRootForDownload() + "/platforms.zip";
        systemImages = pathManager.getRootForDownload() + "/system-images.zip";
        platformToolsZipPath = pathManager.getRootForDownload() + "/platform-tools.zip";
        toolsZipPath = pathManager.getRootForDownload() + "/tools.zip";
        antZipPath = pathManager.getRootForDownload() + "/apache-ant-1.8.0.zip";
    }

    public void downloadPlatform() {
        download("http://dl-ssl.google.com/android/repository/android-16_r04.zip", platformZipPath);  //Same for all platforms
    }

    private void downloadAbi() {
        download("http://dl.google.com/android/repository/sysimg_armv7a-16_r03.zip", systemImages);  //Same for all platforms
    }

    public void downloadPlatformTools() {
        String downloadURL;
        if (SystemInfo.isWindows) {
            downloadURL = "http://dl-ssl.google.com/android/repository/platform-tools_r16-windows.zip";
        }
        else if (SystemInfo.isMac) {
            downloadURL = "http://dl-ssl.google.com/android/repository/platform-tools_r16-macosx.zip";
        }
        else if (SystemInfo.isUnix) {
            downloadURL = "http://dl-ssl.google.com/android/repository/platform-tools_r16-linux.zip";
        }
        else {
            throw new IllegalStateException("Your operating system doesn't supported yet.");
        }
        download(downloadURL, platformToolsZipPath);
    }

    public void downloadTools() {
        String downloadURL;
        if (SystemInfo.isWindows) {
            downloadURL = "http://dl.google.com/android/repository/tools_r16-windows.zip";
        }
        else if (SystemInfo.isMac) {
            downloadURL = "http://dl.google.com/android/repository/tools_r16-macosx.zip";
        }
        else if (SystemInfo.isUnix) {
            downloadURL = "http://dl.google.com/android/repository/tools_r16-linux.zip";
        }
        else {
            throw new IllegalStateException("Your operating system doesn't supported yet.");
        }
        download(downloadURL, toolsZipPath);
    }

    public void downloadAnt() {
        download("http://archive.apache.org/dist/ant/binaries/apache-ant-1.8.0-bin.zip", antZipPath);
    }

    public void downloadAll() {
        downloadTools();
        downloadAbi();
        downloadPlatform();
        downloadPlatformTools();
        downloadAnt();
    }


    public void unzipAll() {
        unzip(platformZipPath, pathManager.getPlatformFolderInAndroidSdk());
        unzip(systemImages, pathManager.getAndroidSdkRoot() + "/system-images/android-16/");
        unzip(platformToolsZipPath, pathManager.getAndroidSdkRoot());
        unzip(toolsZipPath, pathManager.getAndroidSdkRoot());
        unzip(antZipPath, pathManager.getDependenciesRoot());
    }

    public void deleteAll() {
        delete(platformZipPath);
        delete(platformToolsZipPath);
        delete(toolsZipPath);
        delete(antZipPath);
    }

    protected void download(String urlString, String output) {
        System.out.println("Start downloading: " + urlString + " to " + output);
        OutputStream outStream = null;
        URLConnection urlConnection = null;

        InputStream is = null;
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
            ZipInputStream zipinputstream = null;
            ZipEntry zipentry;
            zipinputstream = new ZipInputStream(new FileInputStream(pathToFile));

            zipentry = zipinputstream.getNextEntry();
            try {
                while (zipentry != null) {
                    String entryName = zipentry.getName();
                    int n;
                    File outputFile = new File(outputFolder + "/" + entryName);

                    if (zipentry.isDirectory()) {
                        outputFile.mkdirs();
                        zipinputstream.closeEntry();
                        zipentry = zipinputstream.getNextEntry();
                        continue;
                    }
                    else {
                        File parentFile = outputFile.getParentFile();
                        if (parentFile != null && !parentFile.exists()) {
                            parentFile.mkdirs();
                        }
                        outputFile.createNewFile();
                    }

                    FileOutputStream fileoutputstream = new FileOutputStream(outputFile);
                    try {
                        while ((n = zipinputstream.read(buf, 0, 1024)) > -1) {
                            fileoutputstream.write(buf, 0, n);
                        }
                    }
                    finally {
                        fileoutputstream.close();
                    }
                    zipinputstream.closeEntry();
                    zipentry = zipinputstream.getNextEntry();
                }

                zipinputstream.close();
            }
            catch (IOException e) {
                System.err.println("Entry name: " + zipentry.getName());
                e.printStackTrace();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Finish unzipping: " + pathToFile + " to " + outputFolder);
    }

    protected void delete(String filePath) {
        File file = new File(filePath);
        file.delete();
    }
}

