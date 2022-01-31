FROM ubuntu:20.04

#ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'
#
#ARG ZULU_REPO_VER=1.0.0-3
#
#RUN apt-get -qq update && \
#    apt-get -qq -y --no-install-recommends install gnupg software-properties-common locales curl && \
#    locale-gen en_US.UTF-8 && \
#    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 0x219BD9C9 && \
#    # For zulu-7 & zulu-8
#    curl -sLO https://cdn.azul.com/zulu/bin/zulu-repo_${ZULU_REPO_VER}_all.deb && dpkg -i zulu-repo_${ZULU_REPO_VER}_all.deb && \
#    # For zulu-6 & zulu-9
#    echo "deb http://repos.azulsystems.com/ubuntu stable main" >> /etc/apt/sources.list.d/zulu.list && \
#    #
#    apt-get -qq update && \
#    # Update everything, it can take many times and make build non-reproducible
##    apt-get -qq -y dist-upgrade && \
#    # JDK 6
#    apt-get -qqy install zulu-6=6.22.0.3 && \
#    # JDK 7
#    echo "Package: zulu7-*\nPin: version 7.52.0+11*\nPin-Priority: 1001" > /etc/apt/preferences && \
#    apt-get -qq -y --no-install-recommends install zulu7-jdk=7.0.332-* && \
#    # JDK 8
#    echo "Package: zulu8-*\nPin: version 8.60.0+21*\nPin-Priority: 1001" > /etc/apt/preferences && \
#    apt-get -qq -y --no-install-recommends install zulu8-jdk=8.0.322-* && \
#    # JDK 9
#    apt-get -qqy install zulu-9=9.0.7.1 && \
#    #
#    apt-get -qq -y purge gnupg software-properties-common curl && \
#    apt -y autoremove && \
#    rm -rf /var/lib/apt/lists/* zulu-repo_${ZULU_REPO_VER}_all.deb

# https://www.azul.com/downloads/?version=java-6-lts&os=ubuntu&architecture=x86-64-bit&package=jdk
ARG ZULU6_DEB=https://cdn.azul.com/zulu/bin/zulu6.22.0.3-jdk6.0.119-linux_amd64.deb
# https://www.azul.com/downloads/?version=java-7-lts&os=ubuntu&architecture=x86-64-bit&package=jdk
ARG ZULU7_DEB=https://cdn.azul.com/zulu/bin/zulu7.52.0.11-ca-jdk7.0.332-linux_amd64.deb
# https://www.azul.com/downloads/?version=java-8-lts&os=ubuntu&architecture=x86-64-bit&package=jdk
ARG ZULU8_DEB=https://cdn.azul.com/zulu/bin/zulu8.60.0.21-ca-jdk8.0.322-linux_amd64.deb
# https://www.azul.com/downloads/?version=java-9-sts&os=ubuntu&architecture=x86-64-bit&package=jdk
ARG ZULU9_DEB=https://cdn.azul.com/zulu/bin/zulu9.0.7.1-jdk9.0.7-linux_amd64.deb

# TODO verify files
RUN apt-get -qq update && \
    apt-get -qq -y --no-install-recommends install software-properties-common curl && \
    curl -sLO ${ZULU6_DEB} && dpkg -i $(basename ${ZULU6_DEB}) && rm $(basename ${ZULU6_DEB}) && \
    curl -sLO ${ZULU7_DEB} && dpkg -i $(basename ${ZULU7_DEB}) && rm $(basename ${ZULU7_DEB}) && \
    curl -sLO ${ZULU8_DEB} && dpkg -i $(basename ${ZULU8_DEB}) && rm $(basename ${ZULU8_DEB}) && \
    curl -sLO ${ZULU9_DEB} && dpkg -i $(basename ${ZULU9_DEB}) && rm $(basename ${ZULU9_DEB}) && \
    apt-get -qq -y purge curl

# New naming conventions
ENV JDK6=/usr/lib/jvm/zulu-6-amd64 \
    JDK7=/usr/lib/jvm/zulu7-ca-amd64 \
    JDK8=/usr/lib/jvm/zulu8-ca-amd64 \
    JDK9=/usr/lib/jvm/zulu-9-amd64

#ARG ZULU6=https://cdn.azul.com/zulu/bin/zulu6.22.0.3-jdk6.0.119-linux_x64.tar.gz
#ARG ZULU7=https://cdn.azul.com/zulu/bin/zulu7.52.0.11-ca-jdk7.0.332-linux_x64.tar.gz
#ARG ZULU8=https://cdn.azul.com/zulu/bin/zulu8.60.0.21-ca-jdk8.0.322-linux_x64.tar.gz
#ARG ZULU9=https://cdn.azul.com/zulu/bin/zulu9.0.7.1-jdk9.0.7-linux_x64.tar.gz
#
#RUN apt-get -qq update && \
#    apt-get -qq -y --no-install-recommends install software-properties-common curl && \
#    curl -sLO ${ZULU6} && tar -xf $(basename ${ZULU6}) && rm $(basename ${ZULU6}) && \
#    curl -sLO ${ZULU7} && tar -xf $(basename ${ZULU7}) && rm $(basename ${ZULU7}) && \
#    curl -sLO ${ZULU8} && tar -xf $(basename ${ZULU8}) && rm $(basename ${ZULU8}) && \
#    curl -sLO ${ZULU9} && tar -xf $(basename ${ZULU9}) && rm $(basename ${ZULU9}) && \
#    apt-get -qq -y purge curl
#
## New naming conventions
#ENV JDK6=/zulu6* \
#    JDK7=/zulu7* \
#    JDK8=/zulu8* \
#    JDK9=/zulu9*

ENV JAVA_HOME=$JDK8

# TeamCity JDK old naming conventions
ENV JDK_16=$JDK6 JDK_17=$JDK7 JDK_18=$JDK8 JDK_9=$JDK9
ENV JDK_16_x64=$JDK6 JDK_17_x64=$JDK7 JDK_18_x64=$JDK8 JDK_9_x64=$JDK9

# Dev Environment
RUN apt update && apt install --no-install-recommends -y git ssh # lsof # <- TODO do we need it?
