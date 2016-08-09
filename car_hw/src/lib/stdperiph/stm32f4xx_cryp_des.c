/**
  ******************************************************************************
  * @file    stm32f4xx_cryp_des.c
  * @author  MCD Application Team
  * @version V1.0.0
  * @date    30-September-2011
  * @brief   This file provides high level functions to encrypt and decrypt an 
  *          input message using DES in ECB/CBC modes.
  *          It uses the stm32f4xx_cryp.c/.h drivers to access the STM32F4xx CRYP
  *          peripheral.
  *
  *  @verbatim
  *
  *          ===================================================================
  *                                   How to use this driver
  *          ===================================================================
  *          1. Enable The CRYP controller clock using 
  *            RCC_AHB2PeriphClockCmd(RCC_AHB2Periph_CRYP, ENABLE); function.
  *
  *          2. Encrypt and decrypt using DES in ECB Mode using CRYP_DES_ECB()
  *             function.
  *
  *          3. Encrypt and decrypt using DES in CBC Mode using CRYP_DES_CBC()
  *             function.
  *
  *  @endverbatim
  *
  ******************************************************************************
  * @attention
  *
  * THE PRESENT FIRMWARE WHICH IS FOR GUIDANCE ONLY AIMS AT PROVIDING CUSTOMERS
  * WITH CODING INFORMATION REGARDING THEIR PRODUCTS IN ORDER FOR THEM TO SAVE
  * TIME. AS A RESULT, STMICROELECTRONICS SHALL NOT BE HELD LIABLE FOR ANY
  * DIRECT, INDIRECT OR CONSEQUENTIAL DAMAGES WITH RESPECT TO ANY CLAIMS ARISING
  * FROM THE CONTENT OF SUCH FIRMWARE AND/OR THE USE MADE BY CUSTOMERS OF THE
  * CODING INFORMATION CONTAINED HEREIN IN CONNECTION WITH THEIR PRODUCTS.
  *
  * <h2><center>&copy; COPYRIGHT 2011 STMicroelectronics</center></h2>
  ******************************************************************************
  */

/* Includes ------------------------------------------------------------------*/
#include "stm32f4xx_cryp.h"


/** @addtogroup STM32F4xx_StdPeriph_Driver
  * @{
  */

/** @defgroup CRYP 
  * @brief CRYP driver modules
  * @{
  */

/* Private typedef -----------------------------------------------------------*/
/* Private define ------------------------------------------------------------*/
#define DESBUSY_TIMEOUT    ((uint32_t) 0x00010000)

/* Private macro -------------------------------------------------------------*/
/* Private variables ---------------------------------------------------------*/
/* Private function prototypes -----------------------------------------------*/
/* Private functions ---------------------------------------------------------*/


/** @defgroup CRYP_Private_Functions
  * @{
  */ 

/** @defgroup CRYP_Group8 High Level DES functions
 *  @brief   High Level DES functions 
 *
@verbatim   
 ===============================================================================
                          High Level DES functions
 ===============================================================================
@endverbatim
  * @{
  */

/**
  * @brief  Encrypt and decrypt using DES in ECB Mode
  * @param  Mode: encryption or decryption Mode.
  *           This parameter can be one of the following values:
  *            @arg MODE_ENCRYPT: Encryption
  *            @arg MODE_DECRYPT: Decryption
  * @param  Key: Key used for DES algorithm.
  * @param  Ilength: length of the Input buffer, must be a multiple of 8.
  * @param  Input: pointer to the Input buffer.
  * @param  Output: pointer to the returned buffer.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: Operation done
  *          - ERROR: Operation failed
  */
ErrorStatus CRYP_DES_ECB(uint8_t Mode, uint8_t Key[8], uint8_t *Input, 
                         uint32_t Ilength, uint8_t *Output)
{
  CRYP_InitTypeDef DES_CRYP_InitStructure;
  CRYP_KeyInitTypeDef DES_CRYP_KeyInitStructure;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;
  uint32_t i = 0;

  /* Crypto structures initialisation*/
  CRYP_KeyStructInit(&DES_CRYP_KeyInitStructure);

  /* Crypto Init for Encryption process */
  if( Mode == MODE_ENCRYPT ) /* DES encryption */
  {
     DES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Encrypt;
  }
  else/* if( Mode == MODE_DECRYPT )*/ /* DES decryption */
  {      
     DES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Decrypt;
  }

  DES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_DES_ECB;
  DES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_8b;
  CRYP_Init(&DES_CRYP_InitStructure);

  /* Key Initialisation */
  DES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
  keyaddr+=4;
  DES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
  CRYP_KeyInit(& DES_CRYP_KeyInitStructure);

  /* Flush IN/OUT FIFO */
  CRYP_FIFOFlush();

  /* Enable Crypto processor */
  CRYP_Cmd(ENABLE);

  for(i=0; ((i<Ilength) && (status != ERROR)); i+=8)
  {

    /* Write the Input block in the Input FIFO */
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;

/* Wait until the complete message has been processed */
    counter = 0;
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != DESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {

      /* Read the Output block from the Output FIFO */
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
    }
  }

  /* Disable Crypto */
  CRYP_Cmd(DISABLE);

  return status; 
}

/**
  * @brief  Encrypt and decrypt using DES in CBC Mode
  * @param  Mode: encryption or decryption Mode.
  *          This parameter can be one of the following values:
  *            @arg MODE_ENCRYPT: Encryption
  *            @arg MODE_DECRYPT: Decryption
  * @param  Key: Key used for DES algorithm.
  * @param  InitVectors: Initialisation Vectors used for DES algorithm.
  * @param  Ilength: length of the Input buffer, must be a multiple of 8.
  * @param  Input: pointer to the Input buffer.
  * @param  Output: pointer to the returned buffer.
  * @retval An ErrorStatus enumeration value:
  *          - SUCCESS: Operation done
  *          - ERROR: Operation failed
  */
ErrorStatus CRYP_DES_CBC(uint8_t Mode, uint8_t Key[8], uint8_t InitVectors[8],
                         uint8_t *Input, uint32_t Ilength, uint8_t *Output)
{
  CRYP_InitTypeDef DES_CRYP_InitStructure;
  CRYP_KeyInitTypeDef DES_CRYP_KeyInitStructure;
  CRYP_IVInitTypeDef DES_CRYP_IVInitStructure;
  __IO uint32_t counter = 0;
  uint32_t busystatus = 0;
  ErrorStatus status = SUCCESS;
  uint32_t keyaddr    = (uint32_t)Key;
  uint32_t inputaddr  = (uint32_t)Input;
  uint32_t outputaddr = (uint32_t)Output;
  uint32_t ivaddr     = (uint32_t)InitVectors;
  uint32_t i = 0;

  /* Crypto structures initialisation*/
  CRYP_KeyStructInit(&DES_CRYP_KeyInitStructure);

  /* Crypto Init for Encryption process */
  if(Mode == MODE_ENCRYPT) /* DES encryption */
  {
     DES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Encrypt;
  }
  else /*if(Mode == MODE_DECRYPT)*/ /* DES decryption */
  {
     DES_CRYP_InitStructure.CRYP_AlgoDir  = CRYP_AlgoDir_Decrypt;
  }

  DES_CRYP_InitStructure.CRYP_AlgoMode = CRYP_AlgoMode_DES_CBC;
  DES_CRYP_InitStructure.CRYP_DataType = CRYP_DataType_8b;
  CRYP_Init(&DES_CRYP_InitStructure);

  /* Key Initialisation */
  DES_CRYP_KeyInitStructure.CRYP_Key1Left = __REV(*(uint32_t*)(keyaddr));
  keyaddr+=4;
  DES_CRYP_KeyInitStructure.CRYP_Key1Right= __REV(*(uint32_t*)(keyaddr));
  CRYP_KeyInit(& DES_CRYP_KeyInitStructure);

  /* Initialization Vectors */
  DES_CRYP_IVInitStructure.CRYP_IV0Left = __REV(*(uint32_t*)(ivaddr));
  ivaddr+=4;
  DES_CRYP_IVInitStructure.CRYP_IV0Right= __REV(*(uint32_t*)(ivaddr));
  CRYP_IVInit(&DES_CRYP_IVInitStructure);

  /* Flush IN/OUT FIFO */
  CRYP_FIFOFlush();
  
  /* Enable Crypto processor */
  CRYP_Cmd(ENABLE);

  for(i=0; ((i<Ilength) && (status != ERROR)); i+=8)
  {
    /* Write the Input block in the Input FIFO */
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;
    CRYP_DataIn(*(uint32_t*)(inputaddr));
    inputaddr+=4;

    /* Wait until the complete message has been processed */
    counter = 0;
    do
    {
      busystatus = CRYP_GetFlagStatus(CRYP_FLAG_BUSY);
      counter++;
    }while ((counter != DESBUSY_TIMEOUT) && (busystatus != RESET));

    if (busystatus != RESET)
   {
       status = ERROR;
    }
    else
    {
      /* Read the Output block from the Output FIFO */
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
      *(uint32_t*)(outputaddr) = CRYP_DataOut();
      outputaddr+=4;
    }
  }

  /* Disable Crypto */
  CRYP_Cmd(DISABLE);

  return status; 
}

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/**
  * @}
  */ 

/******************* (C) COPYRIGHT 2011 STMicroelectronics *****END OF FILE****/
